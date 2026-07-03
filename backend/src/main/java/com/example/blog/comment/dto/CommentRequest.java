package com.example.blog.comment.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CommentRequest(
        @NotBlank @Size(min = 2, max = 64) String nickname,
        @Email @Size(max = 160) String email,
        @Size(max = 300) String website,
        @NotBlank @Size(min = 2, max = 2000) String content,
        Long parentId,
        boolean notifyOnReply,
        @Size(max = 64) String captchaId,
        @Size(max = 16) String captchaAnswer
) {
}
