package com.example.blog.notification.model;

public record NotificationOutboxRecord(
        Long id,
        String eventType,
        byte[] recipientCiphertext,
        String templateName,
        String payloadJson,
        String status,
        int retryCount
) {
}
