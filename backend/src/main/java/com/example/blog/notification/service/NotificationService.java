package com.example.blog.notification.service;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.example.blog.notification.mapper.NotificationOutboxMapper;

@Service
public class NotificationService {

    private final NotificationOutboxMapper mapper;
    private final ObjectMapper objectMapper;

    public NotificationService(NotificationOutboxMapper mapper, ObjectMapper objectMapper) {
        this.mapper = mapper;
        this.objectMapper = objectMapper;
    }

    public void enqueueReply(byte[] encryptedEmail, String nickname, String replyPreview, String targetUrl) {
        if (encryptedEmail == null) return;
        try {
            String payload = objectMapper.writeValueAsString(Map.of(
                    "nickname", nickname,
                    "replyPreview", replyPreview,
                    "targetUrl", targetUrl
            ));
            mapper.insert(
                    "COMMENT_REPLY",
                    encryptedEmail,
                    "comment-reply",
                    payload,
                    LocalDateTime.now(ZoneOffset.UTC)
            );
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Cannot serialize notification payload", exception);
        }
    }
}
