package com.example.blog.article.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.example.blog.article.dto.ArticleUpsertRequest;
import com.example.blog.article.mapper.ArticleMapper;
import com.example.blog.article.model.ArticleRecord;
import com.example.blog.shared.error.ApiException;

@ExtendWith(MockitoExtension.class)
class ArticleServiceTests {

    @Mock
    private ArticleMapper articleMapper;

    private ArticleService articleService;

    @BeforeEach
    void setUp() {
        articleService = new ArticleService(articleMapper, new MarkdownService());
    }

    @Test
    void createsDraftWithRenderedMarkdownAndTags() {
        ArticleUpsertRequest request = request(Set.of(3L, 7L));
        when(articleMapper.countExistingTags(request.tagIds())).thenReturn(2);
        when(articleMapper.lastInsertId()).thenReturn(42L);
        when(articleMapper.findById(42L)).thenReturn(Optional.of(article(42L)));
        when(articleMapper.findTagIds(42L)).thenReturn(List.of(3L, 7L));

        var created = articleService.create(request);

        assertEquals(42L, created.id());
        assertEquals(List.of(3L, 7L), created.tagIds());
        verify(articleMapper).insert(
                eq("A useful title"),
                eq("a-useful-title"),
                eq("A short summary"),
                eq("# Hello"),
                eq("<h1>Hello</h1>"),
                eq("Hello"),
                isNull(),
                eq("PUBLIC"),
                eq(false),
                eq(true),
                eq(1),
                eq(1),
                isNull(),
                isNull(),
                isNull()
        );
        verify(articleMapper).insertTags(42L, request.tagIds());
    }

    @Test
    void rejectsUnknownStatusBeforeQuerying() {
        assertThrows(
                ApiException.class,
                () -> articleService.findPage("not-a-status", null, 1, 20)
        );
    }

    @Test
    void reportsOptimisticLockConflict() {
        when(articleMapper.findById(42L)).thenReturn(Optional.of(article(42L)));
        when(articleMapper.update(
                any(), anyString(), anyString(), any(), anyString(), anyString(), anyString(),
                any(), anyString(), eq(false), eq(true), anyInt(), anyInt(),
                any(), any(), any(), eq(0)
        )).thenReturn(0);

        assertThrows(ApiException.class, () -> articleService.update(42L, request(Set.of())));
    }

    @Test
    void rejectsPublishingArticleWithoutTitle() {
        when(articleMapper.findById(42L)).thenReturn(Optional.of(article(42L, " ", "a-useful-title")));

        ApiException exception = assertThrows(ApiException.class, () -> articleService.publish(42L));

        assertEquals("标题不能为空，无法发布", exception.getMessage());
        verify(articleMapper, never()).publish(eq(42L), any());
    }

    @Test
    void rejectsPublishingArticleWithoutSlug() {
        when(articleMapper.findById(42L)).thenReturn(Optional.of(article(42L, "A useful title", " ")));

        ApiException exception = assertThrows(ApiException.class, () -> articleService.publish(42L));

        assertEquals("slug 不能为空，无法发布", exception.getMessage());
        verify(articleMapper, never()).publish(eq(42L), any());
    }

    private ArticleUpsertRequest request(Set<Long> tagIds) {
        return new ArticleUpsertRequest(
                "A useful title",
                "a-useful-title",
                "A short summary",
                "# Hello",
                null,
                tagIds,
                "PUBLIC",
                false,
                true,
                null,
                null,
                null,
                0
        );
    }

    private ArticleRecord article(Long id) {
        return article(id, "A useful title", "a-useful-title");
    }

    private ArticleRecord article(Long id, String title, String slug) {
        LocalDateTime now = LocalDateTime.of(2026, 7, 1, 8, 0);
        return new ArticleRecord(
                id,
                title,
                slug,
                "A short summary",
                "# Hello",
                "<h1>Hello</h1>",
                null,
                "DRAFT",
                "PUBLIC",
                false,
                true,
                1,
                1,
                0,
                null,
                null,
                null,
                null,
                now,
                now,
                0
        );
    }
}
