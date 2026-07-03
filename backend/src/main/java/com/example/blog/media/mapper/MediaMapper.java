package com.example.blog.media.mapper;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.example.blog.media.model.MediaAssetRecord;

@Mapper
public interface MediaMapper {

    List<MediaAssetRecord> findPage(@Param("offset") long offset, @Param("limit") int limit);

    long count();

    Optional<MediaAssetRecord> findById(Long id);

    int insert(
            @Param("objectKey") String objectKey,
            @Param("originalName") String originalName,
            @Param("mediaType") String mediaType,
            @Param("extension") String extension,
            @Param("sizeBytes") long sizeBytes,
            @Param("width") Integer width,
            @Param("height") Integer height,
            @Param("sha256") String sha256,
            @Param("altText") String altText,
            @Param("createdBy") Long createdBy
    );

    Long lastInsertId();

    int updateAltText(@Param("id") Long id, @Param("altText") String altText);

    int reconcileReferences(@Param("now") LocalDateTime now);

    int markOrphans(@Param("cutoff") LocalDateTime cutoff);

    List<MediaAssetRecord> findDeleteCandidates(
            @Param("now") LocalDateTime now,
            @Param("limit") int limit
    );

    int claimManualDelete(
            @Param("id") Long id,
            @Param("now") LocalDateTime now,
            @Param("leaseUntil") LocalDateTime leaseUntil
    );

    int claimLifecycleDelete(
            @Param("id") Long id,
            @Param("expectedStatus") String expectedStatus,
            @Param("now") LocalDateTime now,
            @Param("leaseUntil") LocalDateTime leaseUntil
    );

    int markDeleteComplete(Long id);

    int markDeleteFailed(
            @Param("id") Long id,
            @Param("nextRetryAt") LocalDateTime nextRetryAt,
            @Param("error") String error
    );
}
