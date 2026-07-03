package com.example.blog.article.service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import com.example.blog.article.dto.PublicArchiveMonth;
import com.example.blog.article.dto.PublicArticleCard;
import com.example.blog.article.dto.PublicArticleDetail;
import com.example.blog.article.dto.PublicArticleNavigation;
import com.example.blog.article.dto.PublicArticlePage;
import com.example.blog.article.dto.PublicArticleRecord;
import com.example.blog.article.dto.PublicHomeResponse;
import com.example.blog.article.dto.PublicTaxonomyItem;
import com.example.blog.article.mapper.PublicArticleMapper;
import com.example.blog.shared.error.ApiException;
import com.example.blog.shared.cache.PublicContentCache;
import com.example.blog.shared.config.RuntimeProperties;
import com.fasterxml.jackson.core.type.TypeReference;

@Service
public class PublicArticleService {

    private final PublicArticleMapper mapper;
    private final PublicContentCache cache;
    private final RuntimeProperties runtime;

    public PublicArticleService(
            PublicArticleMapper mapper,
            PublicContentCache cache,
            RuntimeProperties runtime
    ) {
        this.mapper = mapper;
        this.cache = cache;
        this.runtime = runtime;
    }

    public PublicArticlePage findArticles(
            String keyword,
            String categorySlug,
            String tagSlug,
            int page,
            int pageSize
    ) {
        String safeKeyword = normalizeKeyword(keyword);
        String safeCategory = blankToNull(categorySlug);
        String safeTag = blankToNull(tagSlug);
        int safePage = Math.max(page, 1);
        int safePageSize = Math.clamp(pageSize, 1, 50);
        String key = "article:list:" + digest(String.join(
                "|",
                String.valueOf(safeKeyword),
                String.valueOf(safeCategory),
                String.valueOf(safeTag),
                String.valueOf(safePage),
                String.valueOf(safePageSize)
        ));
        return cache.get(
                key,
                runtime.listCacheTtl(),
                PublicArticlePage.class,
                () -> loadArticles(
                        safeKeyword,
                        safeCategory,
                        safeTag,
                        safePage,
                        safePageSize
                )
        );
    }

    public PublicArticleDetail findBySlug(String slug) {
        String normalized = slug == null ? "" : slug.trim();
        return cache.get(
                "article:slug:" + digest(normalized),
                runtime.articleCacheTtl(),
                PublicArticleDetail.class,
                () -> {
                    PublicArticleRecord article = mapper.findBySlug(normalized)
                            .orElseThrow(() -> new ApiException(
                                    HttpStatus.NOT_FOUND,
                                    "文章不存在"
                            ));
                    return withTags(article);
                }
        );
    }

    public Optional<String> findRedirect(String slug) {
        return mapper.findRedirect(slug);
    }

    public PublicHomeResponse home() {
        return cache.get(
                "home",
                runtime.listCacheTtl(),
                PublicHomeResponse.class,
                this::loadHome
        );
    }

    public List<PublicTaxonomyItem> categories() {
        return cache.get(
                "taxonomy:categories",
                runtime.taxonomyCacheTtl(),
                new TypeReference<>() { },
                mapper::findCategories
        );
    }

    public List<PublicTaxonomyItem> tags() {
        return cache.get(
                "taxonomy:tags",
                runtime.taxonomyCacheTtl(),
                new TypeReference<>() { },
                mapper::findTags
        );
    }

    public List<PublicArchiveMonth> archives() {
        return cache.get(
                "archives",
                runtime.listCacheTtl(),
                new TypeReference<>() { },
                this::loadArchives
        );
    }

    private List<PublicArchiveMonth> loadArchives() {
        List<PublicArticleCard> articles = mapper.findArticles(null, null, null, 0, 1000);
        Map<String, List<PublicArticleCard>> groups = new LinkedHashMap<>();
        for (PublicArticleCard article : articles) {
            String key = article.publishedAt().getYear() + "-" + article.publishedAt().getMonthValue();
            groups.computeIfAbsent(key, ignored -> new ArrayList<>()).add(article);
        }
        return groups.values().stream()
                .map(items -> new PublicArchiveMonth(
                        items.getFirst().publishedAt().getYear(),
                        items.getFirst().publishedAt().getMonthValue(),
                        List.copyOf(items)))
                .toList();
    }

    public PublicArticleNavigation adjacent(String slug) {
        return cache.get(
                "article:adjacent:" + digest(slug),
                runtime.listCacheTtl(),
                PublicArticleNavigation.class,
                () -> {
                    PublicArticleDetail article = findBySlug(slug);
                    return new PublicArticleNavigation(
                            mapper.findPrevious(article.publishedAt(), article.id()).orElse(null),
                            mapper.findNext(article.publishedAt(), article.id()).orElse(null)
                    );
                }
        );
    }

    public List<PublicArticleCard> related(String slug) {
        return cache.get(
                "article:related:" + digest(slug),
                runtime.listCacheTtl(),
                new TypeReference<>() { },
                () -> {
                    PublicArticleDetail article = findBySlug(slug);
                    return mapper.findRelated(article.id(), article.categorySlug(), 3);
                }
        );
    }

    private PublicArticlePage loadArticles(
            String keyword,
            String category,
            String tag,
            int page,
            int pageSize
    ) {
        long total = mapper.countArticles(keyword, category, tag);
        long offset = (long) (page - 1) * pageSize;
        int totalPages = total == 0 ? 0 : (int) Math.ceil((double) total / pageSize);
        return new PublicArticlePage(
                mapper.findArticles(keyword, category, tag, offset, pageSize),
                total,
                page,
                pageSize,
                totalPages
        );
    }

    private PublicHomeResponse loadHome() {
        PublicArticlePage page = findArticles(null, null, null, 1, 9);
        List<PublicArticleCard> featured = page.items().stream()
                .filter(PublicArticleCard::pinned)
                .limit(3)
                .toList();
        if (featured.isEmpty()) {
            featured = page.items().stream().limit(3).toList();
        }
        return new PublicHomeResponse(
                featured,
                page.items(),
                categories(),
                tags(),
                page.total()
        );
    }

    private PublicArticleDetail withTags(PublicArticleRecord article) {
        return new PublicArticleDetail(
                article.id(), article.title(), article.slug(), article.summary(),
                article.contentHtml(), article.categoryName(), article.categorySlug(),
                mapper.findArticleTags(article.id()), article.pinned(), article.allowComment(), article.wordCount(),
                article.readingMinutes(), article.viewCount(), article.metaTitle(),
                article.metaDescription(), article.canonicalUrl(), article.publishedAt(),
                article.updatedAt()
        );
    }

    private String normalizeKeyword(String keyword) {
        String normalized = blankToNull(keyword);
        if (normalized != null && normalized.length() > 100) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "搜索关键词不能超过 100 个字符");
        }
        return normalized;
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private String digest(String value) {
        try {
            return HexFormat.of().formatHex(
                    MessageDigest.getInstance("SHA-256")
                            .digest(value.getBytes(StandardCharsets.UTF_8))
            ).substring(0, 32);
        } catch (Exception exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }
}
