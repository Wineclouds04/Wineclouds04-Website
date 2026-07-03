package com.example.blog.comment.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AdminReplyRequest(@NotBlank @Size(min = 2, max = 2000) String content) {
}
