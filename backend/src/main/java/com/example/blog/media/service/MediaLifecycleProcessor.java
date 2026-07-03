package com.example.blog.media.service;

import java.time.LocalDateTime;
import java.time.ZoneOffset;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.example.blog.media.config.MediaLifecycleProperties;
import com.example.blog.media.mapper.MediaMapper;
import com.example.blog.media.model.MediaAssetRecord;
import com.example.blog.media.storage.ObjectStorage;

@Service
public class MediaLifecycleProcessor {

    private static final Logger LOGGER = LoggerFactory.getLogger(MediaLifecycleProcessor.class);

    private final MediaMapper mediaMapper;
    private final MediaService mediaService;
    private final ObjectStorage objectStorage;
    private final MediaLifecycleProperties properties;

    public MediaLifecycleProcessor(
            MediaMapper mediaMapper,
            MediaService mediaService,
            ObjectStorage objectStorage,
            MediaLifecycleProperties properties
    ) {
        this.mediaMapper = mediaMapper;
        this.mediaService = mediaService;
        this.objectStorage = objectStorage;
        this.properties = properties;
    }

    @Scheduled(fixedDelayString = "${blog.media-lifecycle.cleanup-interval:300000}")
    public void reconcileAndCleanup() {
        if (!objectStorage.configured()) return;

        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
        mediaMapper.reconcileReferences(now);
        mediaMapper.markOrphans(now.minus(properties.safeOrphanGracePeriod()));

        for (MediaAssetRecord asset : mediaMapper.findDeleteCandidates(
                now,
                properties.safeCleanupBatchSize()
        )) {
            try {
                mediaService.processDeleteCandidate(asset, now);
            } catch (RuntimeException exception) {
                LOGGER.warn(
                        "OSS media deletion failed; retry scheduled. mediaId={}, attempt={}",
                        asset.id(),
                        asset.deleteAttempts() + 1,
                        exception
                );
            }
        }
    }
}
