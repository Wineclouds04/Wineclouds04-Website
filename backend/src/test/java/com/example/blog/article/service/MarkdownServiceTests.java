package com.example.blog.article.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class MarkdownServiceTests {

    private final MarkdownService markdownService = new MarkdownService();

    @Test
    void rendersGfmAndCountsMixedWords() {
        var result = markdownService.render("""
                # 标题

                Hello world.

                | A | B |
                | - | - |
                | 1 | 2 |
                """);

        assertTrue(result.html().contains("<table>"));
        assertTrue(result.html().contains("<h1>标题</h1>"));
        assertEquals(8, result.wordCount());
        assertEquals(1, result.readingMinutes());
    }

    @Test
    void escapesRawHtmlAndHardensExternalLinks() {
        var result = markdownService.render("""
                <script>alert('xss')</script>

                [outside](https://example.com)
                """);

        assertFalse(result.html().contains("<script>"));
        assertTrue(result.html().contains("&lt;script&gt;"));
        assertTrue(result.html().contains("rel=\"noopener noreferrer nofollow\""));
    }
}
