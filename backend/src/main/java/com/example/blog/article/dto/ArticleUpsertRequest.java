package com.example.blog.article.dto;

import java.util.Set;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

public record ArticleUpsertRequest(
        @NotBlank @Size(max = 200) String title,
        @NotBlank
        @Size(max = 160)
        @Pattern(
                regexp = "[a-z0-9]+(?:-[a-z0-9]+)*",
                message = "只能包含小写字母、数字和连字符"
        )
        String slug,
        @Size(max = 600) String summary,
        @NotNull String contentMarkdown,
        Long categoryId,
        @NotNull @Size(max = 20) Set<Long> tagIds,
        @NotBlank @Pattern(regexp = "PUBLIC|PRIVATE") String visibility,
        boolean pinned,
        boolean allowComment,
        @Size(max = 200) String metaTitle,
        @Size(max = 320) String metaDescription,
        @Size(max = 500) String canonicalUrl,
        @PositiveOrZero int version
) {
}
