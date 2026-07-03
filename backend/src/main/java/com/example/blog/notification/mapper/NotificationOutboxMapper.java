package com.example.blog.notification.mapper;

import java.time.LocalDateTime;
import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.example.blog.notification.model.NotificationOutboxRecord;

@Mapper
public interface NotificationOutboxMapper {

    int insert(
            @Param("eventType") String eventType,
            @Param("recipientCiphertext") byte[] recipientCiphertext,
            @Param("templateName") String templateName,
            @Param("payloadJson") String payloadJson,
            @Param("nextAttemptAt") LocalDateTime nextAttemptAt
    );

    List<NotificationOutboxRecord> findPending(int limit);

    int markSent(Long id);

    int markRetry(
            @Param("id") Long id,
            @Param("status") String status,
            @Param("nextAttemptAt") LocalDateTime nextAttemptAt,
            @Param("lastError") String lastError
    );

    long countPending();
}
