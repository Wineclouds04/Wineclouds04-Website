package com.example.blog.comment.mapper;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.example.blog.comment.model.AdminCommentRecord;
import com.example.blog.comment.model.CommentRecord;

@Mapper
public interface CommentMapper {

    boolean articleAcceptsComments(Long articleId);

    List<CommentRecord> findApprovedArticleComments(Long articleId);

    List<CommentRecord> findApprovedMessages();

    Optional<CommentRecord> findById(Long id);

    Optional<String> findArticleSlug(Long articleId);

    int countDuplicate(
            @Param("anonymousKeyHash") String anonymousKeyHash,
            @Param("contentMarkdown") String contentMarkdown,
            @Param("createdAfter") LocalDateTime createdAfter
    );

    int countByAnonymousKey(String anonymousKeyHash);

    int insert(
            @Param("articleId") Long articleId,
            @Param("rootId") Long rootId,
            @Param("parentId") Long parentId,
            @Param("type") String type,
            @Param("contentMarkdown") String contentMarkdown,
            @Param("contentHtml") String contentHtml,
            @Param("nickname") String nickname,
            @Param("emailCiphertext") byte[] emailCiphertext,
            @Param("website") String website,
            @Param("anonymousKeyHash") String anonymousKeyHash,
            @Param("status") String status,
            @Param("adminReply") boolean adminReply,
            @Param("notifyOnReply") boolean notifyOnReply,
            @Param("ipHash") String ipHash,
            @Param("userAgentSummary") String userAgentSummary
    );

    Long lastInsertId();

    List<AdminCommentRecord> findAdminPage(
            @Param("status") String status,
            @Param("type") String type,
            @Param("keyword") String keyword,
            @Param("offset") long offset,
            @Param("limit") int limit
    );

    long countAdminPage(
            @Param("status") String status,
            @Param("type") String type,
            @Param("keyword") String keyword
    );

    int updateStatus(@Param("id") Long id, @Param("status") String status);

    int softDelete(Long id);

    int refreshArticleCommentCount(Long articleId);
}
