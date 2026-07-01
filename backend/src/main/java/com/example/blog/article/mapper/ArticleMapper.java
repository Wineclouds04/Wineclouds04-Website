package com.example.blog.article.mapper;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.example.blog.article.dto.ArticleListItem;
import com.example.blog.article.dto.TaxonomyOption;
import com.example.blog.article.model.ArticleRecord;

@Mapper
public interface ArticleMapper {

    List<ArticleListItem> findPage(
            @Param("status") String status,
            @Param("keyword") String keyword,
            @Param("offset") long offset,
            @Param("limit") int limit
    );

    long count(@Param("status") String status, @Param("keyword") String keyword);

    Optional<ArticleRecord> findById(Long id);

    List<Long> findTagIds(Long articleId);

    boolean slugExists(@Param("slug") String slug, @Param("excludeId") Long excludeId);

    boolean categoryExists(Long id);

    int countExistingTags(Set<Long> ids);

    List<TaxonomyOption> findCategories();

    List<TaxonomyOption> findTags();

    int insert(
            @Param("title") String title,
            @Param("slug") String slug,
            @Param("summary") String summary,
            @Param("markdown") String markdown,
            @Param("html") String html,
            @Param("plain") String plain,
            @Param("categoryId") Long categoryId,
            @Param("visibility") String visibility,
            @Param("pinned") boolean pinned,
            @Param("allowComment") boolean allowComment,
            @Param("wordCount") int wordCount,
            @Param("readingMinutes") int readingMinutes,
            @Param("metaTitle") String metaTitle,
            @Param("metaDescription") String metaDescription,
            @Param("canonicalUrl") String canonicalUrl
    );

    Long lastInsertId();

    int update(
            @Param("id") Long id,
            @Param("title") String title,
            @Param("slug") String slug,
            @Param("summary") String summary,
            @Param("markdown") String markdown,
            @Param("html") String html,
            @Param("plain") String plain,
            @Param("categoryId") Long categoryId,
            @Param("visibility") String visibility,
            @Param("pinned") boolean pinned,
            @Param("allowComment") boolean allowComment,
            @Param("wordCount") int wordCount,
            @Param("readingMinutes") int readingMinutes,
            @Param("metaTitle") String metaTitle,
            @Param("metaDescription") String metaDescription,
            @Param("canonicalUrl") String canonicalUrl,
            @Param("version") int version
    );

    void deleteTags(Long articleId);

    void insertTags(@Param("articleId") Long articleId, @Param("tagIds") Set<Long> tagIds);

    int publish(@Param("id") Long id, @Param("publishedAt") LocalDateTime publishedAt);

    int withdraw(Long id);

    int softDelete(Long id);

    void insertRedirect(@Param("oldSlug") String oldSlug, @Param("targetPath") String targetPath);
}
