package com.example.blog.media.storage;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.util.StringUtils;

import com.aliyun.sdk.service.oss2.OSSClient;
import com.aliyun.sdk.service.oss2.OSSClientBuilder;
import com.aliyun.sdk.service.oss2.credentials.EnvironmentVariableCredentialsProvider;
import com.aliyun.sdk.service.oss2.models.DeleteObjectRequest;
import com.aliyun.sdk.service.oss2.models.PutObjectRequest;
import com.aliyun.sdk.service.oss2.transport.BinaryData;
import com.example.blog.media.config.OssProperties;
import com.example.blog.shared.error.ApiException;

public class AliyunOssObjectStorage implements ObjectStorage {

    private final OssProperties properties;
    private volatile OSSClient client;

    public AliyunOssObjectStorage(OssProperties properties) {
        this.properties = properties;
    }

    @Override
    public boolean configured() {
        return properties.configured();
    }

    @Override
    public void put(String objectKey, byte[] content, String contentType) {
        requireConfigured();
        try {
            client().putObject(PutObjectRequest.newBuilder()
                    .bucket(properties.bucket())
                    .key(objectKey)
                    .contentType(contentType)
                    .forbidOverwrite(true)
                    .body(BinaryData.fromBytes(content))
                    .build());
        } catch (Exception exception) {
            throw new ApiException(HttpStatus.BAD_GATEWAY, "OSS 上传失败，请稍后重试");
        }
    }

    @Override
    public void delete(String objectKey) {
        requireConfigured();
        try {
            client().deleteObject(DeleteObjectRequest.newBuilder()
                    .bucket(properties.bucket())
                    .key(objectKey)
                    .build());
        } catch (Exception exception) {
            throw new ApiException(HttpStatus.BAD_GATEWAY, "OSS 删除失败，请稍后重试");
        }
    }

    @Override
    public String publicUrl(String objectKey) {
        String encodedKey = Arrays.stream(objectKey.split("/"))
                .map(part -> URLEncoder.encode(part, StandardCharsets.UTF_8))
                .collect(Collectors.joining("/"));
        if (StringUtils.hasText(properties.cdnDomain())) {
            return trimTrailingSlash(properties.cdnDomain()) + "/" + encodedKey;
        }
        if (StringUtils.hasText(properties.endpoint())) {
            String endpoint = trimTrailingSlash(properties.endpoint());
            String scheme = endpoint.startsWith("http://") || endpoint.startsWith("https://")
                    ? ""
                    : "https://";
            return scheme + properties.bucket() + "." + endpoint.replaceFirst("^https?://", "")
                    + "/" + encodedKey;
        }
        return "https://" + properties.bucket() + ".oss-" + properties.region()
                + ".aliyuncs.com/" + encodedKey;
    }

    @Override
    public void close() {
        OSSClient current = client;
        if (current != null) {
            try {
                current.close();
            } catch (Exception ignored) {
                // Client shutdown is best-effort during application termination.
            }
        }
    }

    private OSSClient client() {
        OSSClient current = client;
        if (current != null) return current;
        synchronized (this) {
            if (client == null) {
                OSSClientBuilder builder = OSSClient.newBuilder()
                        .credentialsProvider(new EnvironmentVariableCredentialsProvider())
                        .region(properties.region());
                if (StringUtils.hasText(properties.endpoint())) {
                    builder.endpoint(properties.endpoint());
                }
                client = builder.build();
            }
            return client;
        }
    }

    private void requireConfigured() {
        if (!configured()) {
            throw new ApiException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "OSS 尚未配置，请设置地域、Bucket 和访问凭据"
            );
        }
    }

    private String trimTrailingSlash(String value) {
        return value.replaceFirst("/+$", "");
    }
}
