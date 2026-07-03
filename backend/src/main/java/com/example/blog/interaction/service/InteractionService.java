package com.example.blog.interaction.service;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.blog.interaction.dto.InteractionState;
import com.example.blog.interaction.mapper.InteractionMapper;
import com.example.blog.shared.error.ApiException;

@Service
public class InteractionService {

    private final InteractionMapper mapper;
    private final PublicInteractionGuard guard;

    public InteractionService(InteractionMapper mapper, PublicInteractionGuard guard) {
        this.mapper = mapper;
        this.guard = guard;
    }

    public InteractionState state(Long articleId, String visitorHash) {
        requireArticle(articleId);
        return mapper.findState(articleId, visitorHash);
    }

    @Transactional
    public InteractionState like(Long articleId, String visitorHash) {
        requireArticle(articleId);
        guard.like(visitorHash);
        mapper.insertLike(articleId, visitorHash);
        mapper.refreshLikeCount(articleId);
        return mapper.findState(articleId, visitorHash);
    }

    @Transactional
    public InteractionState unlike(Long articleId, String visitorHash) {
        requireArticle(articleId);
        mapper.deleteLike(articleId, visitorHash);
        mapper.refreshLikeCount(articleId);
        return mapper.findState(articleId, visitorHash);
    }

    private void requireArticle(Long articleId) {
        if (!mapper.articleIsPublic(articleId)) {
            throw new ApiException(HttpStatus.NOT_FOUND, "文章不存在");
        }
    }
}
