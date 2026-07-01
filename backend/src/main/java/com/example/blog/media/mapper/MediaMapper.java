package com.example.blog.media.mapper;

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

    int softDelete(Long id);
}
