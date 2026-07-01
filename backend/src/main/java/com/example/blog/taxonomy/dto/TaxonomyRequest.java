package com.example.blog.taxonomy.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

public record TaxonomyRequest(
        @NotBlank @Size(max = 64) String name,
        @NotBlank
        @Size(max = 80)
        @Pattern(
                regexp = "[a-z0-9]+(?:-[a-z0-9]+)*",
                message = "只能包含小写字母、数字和连字符"
        )
        String slug,
        @Size(max = 300) String description,
        @PositiveOrZero int sortOrder,
        boolean visible
) {
}
