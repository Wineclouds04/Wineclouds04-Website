package com.example.blog.notification.service;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Map;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.example.blog.notification.config.MailNotificationProperties;
import com.example.blog.notification.mapper.NotificationOutboxMapper;
import com.example.blog.notification.model.NotificationOutboxRecord;

@Service
public class MailOutboxProcessor {

    private final NotificationOutboxMapper mapper;
    private final SensitiveDataCipher cipher;
    private final ObjectMapper objectMapper;
    private final JavaMailSender mailSender;
    private final MailNotificationProperties properties;

    public MailOutboxProcessor(
            NotificationOutboxMapper mapper,
            SensitiveDataCipher cipher,
            ObjectMapper objectMapper,
            ObjectProvider<JavaMailSender> mailSender,
            MailNotificationProperties properties
    ) {
        this.mapper = mapper;
        this.cipher = cipher;
        this.objectMapper = objectMapper;
        this.mailSender = mailSender.getIfAvailable();
        this.properties = properties;
    }

    public boolean configured() {
        return properties.enabled()
                && mailSender != null
                && properties.from() != null
                && !properties.from().isBlank();
    }

    @Scheduled(fixedDelayString = "${blog.mail.poll-interval:30000}")
    public void deliverPending() {
        if (!configured()) return;
        for (NotificationOutboxRecord item : mapper.findPending(20)) {
            deliver(item);
        }
    }

    private void deliver(NotificationOutboxRecord item) {
        try {
            Map<String, String> payload = objectMapper.readValue(
                    item.payloadJson(),
                    new TypeReference<>() { }
            );
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(properties.from());
            message.setTo(cipher.decrypt(item.recipientCiphertext()));
            message.setSubject("你在余白札记收到了新回复");
            message.setText("""
                    %s，你好：

                    你的评论收到了一条新回复：
                    %s

                    查看详情：%s
                    """.formatted(
                    payload.getOrDefault("nickname", "朋友"),
                    payload.getOrDefault("replyPreview", ""),
                    payload.getOrDefault("targetUrl", properties.siteUrl())
            ));
            mailSender.send(message);
            mapper.markSent(item.id());
        } catch (Exception exception) {
            int maxRetries = Math.max(1, properties.maxRetries());
            String status = item.retryCount() + 1 >= maxRetries ? "FAILED" : "RETRY";
            String error = exception.getMessage() == null
                    ? exception.getClass().getSimpleName()
                    : exception.getMessage();
            mapper.markRetry(
                    item.id(),
                    status,
                    LocalDateTime.now(ZoneOffset.UTC).plusMinutes(1L << Math.min(item.retryCount(), 6)),
                    error.substring(0, Math.min(error.length(), 1000))
            );
        }
    }
}
