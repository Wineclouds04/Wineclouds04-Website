package com.example.blog.comment.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.blog.article.service.MarkdownService;
import com.example.blog.comment.dto.AdminCommentPage;
import com.example.blog.comment.dto.AdminCommentItem;
import com.example.blog.comment.dto.CommentRequest;
import com.example.blog.comment.dto.CommentSubmitResponse;
import com.example.blog.comment.dto.PublicCommentResponse;
import com.example.blog.comment.mapper.CommentMapper;
import com.example.blog.comment.model.CommentRecord;
import com.example.blog.interaction.service.PublicInteractionGuard;
import com.example.blog.interaction.service.VisitorContext.Visitor;
import com.example.blog.notification.config.MailNotificationProperties;
import com.example.blog.notification.service.NotificationService;
import com.example.blog.notification.service.SensitiveDataCipher;
import com.example.blog.operation.service.OperationLogService;
import com.example.blog.shared.error.ApiException;
import com.example.blog.shared.security.CaptchaService;

@Service
public class CommentService {

    private static final Set<String> STATUSES =
            Set.of("PENDING", "APPROVED", "REJECTED", "SPAM", "HIDDEN");
    private static final Set<String> TYPES = Set.of("ARTICLE", "MESSAGE");
    private static final Pattern LINK_PATTERN = Pattern.compile("(?i)(https?://|www\\.)");

    private final CommentMapper mapper;
    private final MarkdownService markdownService;
    private final SensitiveDataCipher cipher;
    private final PublicInteractionGuard guard;
    private final NotificationService notifications;
    private final MailNotificationProperties mailProperties;
    private final OperationLogService operationLogs;
    private final CaptchaService captchas;

    public CommentService(
            CommentMapper mapper,
            MarkdownService markdownService,
            SensitiveDataCipher cipher,
            PublicInteractionGuard guard,
            NotificationService notifications,
            MailNotificationProperties mailProperties,
            OperationLogService operationLogs,
            CaptchaService captchas
    ) {
        this.mapper = mapper;
        this.markdownService = markdownService;
        this.cipher = cipher;
        this.guard = guard;
        this.notifications = notifications;
        this.mailProperties = mailProperties;
        this.operationLogs = operationLogs;
        this.captchas = captchas;
    }

    public List<PublicCommentResponse> articleComments(Long articleId) {
        return tree(mapper.findApprovedArticleComments(articleId));
    }

    public List<PublicCommentResponse> messages() {
        return tree(mapper.findApprovedMessages());
    }

    @Transactional
    public CommentSubmitResponse submitArticle(Long articleId, CommentRequest request, Visitor visitor) {
        if (!mapper.articleAcceptsComments(articleId)) {
            throw new ApiException(HttpStatus.NOT_FOUND, "文章不存在或未开放评论");
        }
        return submit(articleId, "ARTICLE", request, visitor);
    }

    @Transactional
    public CommentSubmitResponse submitMessage(CommentRequest request, Visitor visitor) {
        return submit(null, "MESSAGE", request, visitor);
    }

    public AdminCommentPage adminPage(
            String status,
            String type,
            String keyword,
            int page,
            int pageSize
    ) {
        String normalizedStatus = normalize(status, STATUSES, "评论状态");
        String normalizedType = normalize(type, TYPES, "互动类型");
        String normalizedKeyword = blankToNull(keyword);
        int safePage = Math.max(1, page);
        int safeSize = Math.clamp(pageSize, 1, 100);
        long offset = (long) (safePage - 1) * safeSize;
        return new AdminCommentPage(
                mapper.findAdminPage(
                        normalizedStatus,
                        normalizedType,
                        normalizedKeyword,
                        offset,
                        safeSize
                ).stream().map(this::toAdminItem).toList(),
                mapper.countAdminPage(normalizedStatus, normalizedType, normalizedKeyword),
                safePage,
                safeSize
        );
    }

    @Transactional
    public void moderate(Long id, String status, Long operatorId) {
        if (!STATUSES.contains(status) || "PENDING".equals(status)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "无效的审核状态");
        }
        CommentRecord comment = require(id);
        if (mapper.updateStatus(id, status) == 0) {
            throw new ApiException(HttpStatus.NOT_FOUND, "评论不存在");
        }
        if (comment.articleId() != null) mapper.refreshArticleCommentCount(comment.articleId());
        if ("APPROVED".equals(status)) enqueueParentNotification(comment);
        operationLogs.record(
                operatorId,
                "COMMENT",
                status,
                id,
                "{\"type\":\"" + comment.type() + "\"}"
        );
    }

    @Transactional
    public void delete(Long id, Long operatorId) {
        CommentRecord comment = require(id);
        if (mapper.softDelete(id) == 0) {
            throw new ApiException(HttpStatus.NOT_FOUND, "评论不存在");
        }
        if (comment.articleId() != null) mapper.refreshArticleCommentCount(comment.articleId());
        operationLogs.record(operatorId, "COMMENT", "DELETE", id, "{}");
    }

    @Transactional
    public PublicCommentResponse adminReply(Long id, String content, Long operatorId) {
        CommentRecord parent = require(id);
        String normalized = validateContent(content);
        var rendered = markdownService.render(normalized);
        Long rootId = parent.rootId() == null ? parent.id() : parent.rootId();
        mapper.insert(
                parent.articleId(),
                rootId,
                rootId,
                parent.type(),
                normalized,
                rendered.html(),
                "博主",
                null,
                null,
                sha256("admin:" + operatorId),
                "APPROVED",
                true,
                false,
                "0".repeat(64),
                "admin"
        );
        Long replyId = mapper.lastInsertId();
        if (parent.articleId() != null) mapper.refreshArticleCommentCount(parent.articleId());
        enqueue(parent, normalized);
        operationLogs.record(operatorId, "COMMENT", "REPLY", replyId, "{\"parentId\":" + id + "}");
        CommentRecord reply = require(replyId);
        return toPublic(reply, List.of());
    }

    private CommentSubmitResponse submit(
            Long articleId,
            String type,
            CommentRequest request,
            Visitor visitor
    ) {
        boolean message = "MESSAGE".equals(type);
        guard.comment(visitor.anonymousKeyHash(), message);
        String content = validateContent(request.content());
        boolean firstInteraction = mapper.countByAnonymousKey(visitor.anonymousKeyHash()) == 0;
        boolean riskyContent = LINK_PATTERN.matcher(content).find();
        if (firstInteraction || riskyContent) {
            captchas.verify(request.captchaId(), request.captchaAnswer());
        }
        if (mapper.countDuplicate(
                visitor.anonymousKeyHash(),
                content,
                LocalDateTime.now(ZoneOffset.UTC).minusHours(24)
        ) > 0) {
            throw new ApiException(HttpStatus.CONFLICT, "请勿重复提交相同内容");
        }

        Long rootId = null;
        Long parentId = null;
        if (request.parentId() != null) {
            CommentRecord parent = require(request.parentId());
            if (!type.equals(parent.type())
                    || !java.util.Objects.equals(articleId, parent.articleId())
                    || !"APPROVED".equals(parent.status())) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "回复目标无效");
            }
            rootId = parent.rootId() == null ? parent.id() : parent.rootId();
            parentId = rootId;
        }

        var rendered = markdownService.render(content);
        mapper.insert(
                articleId,
                rootId,
                parentId,
                type,
                content,
                rendered.html(),
                request.nickname().trim(),
                cipher.encrypt(blankToNull(request.email())),
                normalizeWebsite(request.website()),
                visitor.anonymousKeyHash(),
                "PENDING",
                false,
                request.notifyOnReply() && blankToNull(request.email()) != null,
                visitor.ipHash(),
                visitor.userAgentSummary()
        );
        Long id = mapper.lastInsertId();
        return new CommentSubmitResponse(id, "PENDING", "已提交，审核通过后会公开显示");
    }

    private List<PublicCommentResponse> tree(List<CommentRecord> records) {
        Map<Long, List<CommentRecord>> replies = new LinkedHashMap<>();
        List<CommentRecord> roots = new ArrayList<>();
        for (CommentRecord item : records) {
            if (item.rootId() == null && item.parentId() == null) {
                roots.add(item);
            } else {
                Long rootId = item.rootId() != null ? item.rootId() : item.parentId();
                replies.computeIfAbsent(rootId, ignored -> new ArrayList<>()).add(item);
            }
        }
        return roots.stream()
                .map(root -> toPublic(
                        root,
                        replies.getOrDefault(root.id(), List.of()).stream()
                                .map(reply -> toPublic(reply, List.of()))
                                .toList()
                ))
                .toList();
    }

    private PublicCommentResponse toPublic(CommentRecord item, List<PublicCommentResponse> replies) {
        return new PublicCommentResponse(
                item.id(),
                item.parentId(),
                item.nickname(),
                item.contentHtml(),
                item.adminReply(),
                item.createdAt(),
                replies
        );
    }

    private AdminCommentItem toAdminItem(
            com.example.blog.comment.model.AdminCommentRecord item
    ) {
        return new AdminCommentItem(
                item.id(),
                item.articleId(),
                item.articleTitle(),
                item.parentId(),
                item.type(),
                item.contentMarkdown(),
                item.nickname(),
                maskEmail(item.emailCiphertext()),
                item.website(),
                item.status(),
                item.adminReply(),
                item.ipSummary(),
                item.createdAt()
        );
    }

    private String maskEmail(byte[] encrypted) {
        if (encrypted == null) return null;
        try {
            String email = cipher.decrypt(encrypted);
            int at = email == null ? -1 : email.indexOf('@');
            if (at <= 0 || at == email.length() - 1) return "***";
            return email.substring(0, 1) + "***" + email.substring(at);
        } catch (RuntimeException exception) {
            return "已加密";
        }
    }

    private void enqueueParentNotification(CommentRecord comment) {
        if (comment.parentId() == null) return;
        mapper.findById(comment.parentId())
                .filter(CommentRecord::notifyOnReply)
                .ifPresent(parent -> enqueue(parent, comment.contentMarkdown()));
    }

    private void enqueue(CommentRecord target, String reply) {
        if (!target.notifyOnReply() || target.emailCiphertext() == null) return;
        String path = target.articleId() == null
                ? "/message#message-" + target.id()
                : "/article/" + mapper.findArticleSlug(target.articleId()).orElse("")
                    + "#comment-" + target.id();
        String base = mailProperties.siteUrl() == null ? "http://localhost" : mailProperties.siteUrl();
        notifications.enqueueReply(
                target.emailCiphertext(),
                target.nickname(),
                reply.length() <= 160 ? reply : reply.substring(0, 160) + "…",
                base.replaceAll("/+$", "") + path
        );
    }

    private CommentRecord require(Long id) {
        return mapper.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "评论不存在"));
    }

    private String validateContent(String value) {
        String content = value == null ? "" : value.trim();
        if (content.length() < 2 || content.length() > 2000) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "评论内容需要 2–2000 个字符");
        }
        if (LINK_PATTERN.matcher(content).results().count() > 2) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "评论中的链接不能超过 2 个");
        }
        return content;
    }

    private String normalizeWebsite(String value) {
        String website = blankToNull(value);
        if (website == null) return null;
        if (!website.startsWith("http://") && !website.startsWith("https://")) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "个人网站必须以 http:// 或 https:// 开头");
        }
        return website;
    }

    private String normalize(String value, Set<String> allowed, String label) {
        String normalized = blankToNull(value);
        if (normalized == null) return null;
        normalized = normalized.toUpperCase();
        if (!allowed.contains(normalized)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, label + "无效");
        }
        return normalized;
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private String sha256(String value) {
        try {
            return HexFormat.of().formatHex(
                    MessageDigest.getInstance("SHA-256")
                            .digest(value.getBytes(StandardCharsets.UTF_8))
            );
        } catch (Exception exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }
}
