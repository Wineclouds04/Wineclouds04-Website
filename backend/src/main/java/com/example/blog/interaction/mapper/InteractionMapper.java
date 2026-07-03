package com.example.blog.interaction.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.example.blog.interaction.dto.InteractionState;

@Mapper
public interface InteractionMapper {

    boolean articleIsPublic(Long articleId);

    InteractionState findState(
            @Param("articleId") Long articleId,
            @Param("anonymousKeyHash") String anonymousKeyHash
    );

    int insertLike(
            @Param("articleId") Long articleId,
            @Param("anonymousKeyHash") String anonymousKeyHash
    );

    int deleteLike(
            @Param("articleId") Long articleId,
            @Param("anonymousKeyHash") String anonymousKeyHash
    );

    int refreshLikeCount(Long articleId);
}
