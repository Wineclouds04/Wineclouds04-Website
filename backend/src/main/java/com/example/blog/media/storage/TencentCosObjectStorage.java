package com.example.blog.media.storage;

import java.io.ByteArrayInputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.example.blog.media.config.CosProperties;
import com.example.blog.shared.error.ApiException;
import com.qcloud.cos.COSClient;
import com.qcloud.cos.ClientConfig;
import com.qcloud.cos.auth.BasicCOSCredentials;
import com.qcloud.cos.auth.COSCredentials;
import com.qcloud.cos.exception.CosServiceException;
import com.qcloud.cos.model.DeleteObjectRequest;
import com.qcloud.cos.model.ObjectMetadata;
import com.qcloud.cos.model.PutObjectRequest;
import com.qcloud.cos.region.Region;

public class TencentCosObjectStorage implements ObjectStorage {

    private static final Logger LOGGER = LoggerFactory.getLogger(TencentCosObjectStorage.class);

    private final CosProperties properties;
    private volatile COSClient client;

    public TencentCosObjectStorage(CosProperties properties) {
        this.properties = properties;
    }

    @Override
    public boolean configured() {
        return properties.configured();
    }

    @Override
    public void put(String objectKey, byte[] content, String contentType) {
        requireConfigured();
        try (ByteArrayInputStream input = new ByteArrayInputStream(content)) {
            ObjectMetadata metadata = new ObjectMetadata();
            metadata.setContentLength(content.length);
            metadata.setContentType(contentType);
            client().putObject(new PutObjectRequest(properties.bucket(), objectKey, input, metadata));
        } catch (Exception exception) {
            logFailure("upload", objectKey, exception);
            throw new ApiException(HttpStatus.BAD_GATEWAY, "COS 上传失败，请稍后重试");
        }
    }

    @Override
    public void delete(String objectKey) {
        requireConfigured();
        try {
            client().deleteObject(new DeleteObjectRequest(properties.bucket(), objectKey));
        } catch (Exception exception) {
            logFailure("delete", objectKey, exception);
            throw new ApiException(HttpStatus.BAD_GATEWAY, "COS 删除失败，请稍后重试");
        }
    }

    @Override
    public String publicUrl(String objectKey) {
        String encodedKey = Arrays.stream(objectKey.split("/"))
                .map(part -> URLEncoder.encode(part, StandardCharsets.UTF_8).replace("+", "%20"))
                .collect(Collectors.joining("/"));
        if (StringUtils.hasText(properties.cdnDomain())) {
            return trimTrailingSlash(properties.cdnDomain()) + "/" + encodedKey;
        }
        return "https://" + properties.bucket() + ".cos." + properties.region()
                + ".myqcloud.com/" + encodedKey;
    }

    @Override
    public void close() {
        COSClient current = client;
        if (current != null) {
            try {
                current.shutdown();
            } catch (Exception ignored) {
                // Client shutdown is best-effort during application termination.
            }
        }
    }

    private COSClient client() {
        COSClient current = client;
        if (current != null) return current;
        synchronized (this) {
            if (client == null) {
                COSCredentials credentials = new BasicCOSCredentials(properties.secretId(), properties.secretKey());
                client = new COSClient(credentials, new ClientConfig(new Region(properties.region())));
            }
            return client;
        }
    }

    private void requireConfigured() {
        if (!configured()) {
            throw new ApiException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "COS 尚未配置，请设置地域、Bucket 和访问凭据"
            );
        }
    }

    private void logFailure(String operation, String objectKey, Exception exception) {
        if (exception instanceof CosServiceException serviceException) {
            LOGGER.warn(
                    "COS {} failed. bucket={}, objectKey={}, statusCode={}, errorCode={}, requestId={}, errorMessage={}",
                    operation,
                    properties.bucket(),
                    objectKey,
                    serviceException.getStatusCode(),
                    serviceException.getErrorCode(),
                    serviceException.getRequestId(),
                    serviceException.getErrorMessage()
            );
            return;
        }
        LOGGER.warn(
                "COS {} failed. bucket={}, objectKey={}, errorType={}, errorMessage={}",
                operation,
                properties.bucket(),
                objectKey,
                exception.getClass().getSimpleName(),
                exception.getMessage()
        );
    }

    private String trimTrailingSlash(String value) {
        return value.replaceFirst("/+$", "");
    }
}
