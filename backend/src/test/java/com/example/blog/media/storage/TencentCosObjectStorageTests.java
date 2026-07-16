package com.example.blog.media.storage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;

import java.lang.reflect.Field;

import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

import com.example.blog.media.config.CosProperties;
import com.example.blog.shared.error.ApiException;
import com.qcloud.cos.COSClient;
import com.qcloud.cos.model.PutObjectRequest;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;

class TencentCosObjectStorageTests {

    @Test
    void buildsCosPublicUrlAndRequiresCredentials() {
        CosProperties properties = new CosProperties(
                "ap-shanghai",
                "blog-1250000000",
                "secret-id",
                "secret-key",
                "",
                "blog/prod",
                10_485_760,
                12_000,
                12_000
        );

        TencentCosObjectStorage storage = new TencentCosObjectStorage(properties);

        assertThat(storage.configured()).isTrue();
        assertThat(storage.publicUrl("blog/prod/hello world.png"))
                .isEqualTo("https://blog-1250000000.cos.ap-shanghai.myqcloud.com/blog/prod/hello%20world.png");
        assertThat(new CosProperties("ap-shanghai", "blog-1250000000", "", "", "", "blog/prod", 1, 1, 1)
                .configured()).isFalse();
    }

    @Test
    void logsTheCosFailureDetailsWithoutChangingThePublicError() throws Exception {
        CosProperties properties = new CosProperties(
                "ap-shanghai",
                "blog-1250000000",
                "secret-id",
                "secret-key",
                "",
                "blog/prod",
                10_485_760,
                12_000,
                12_000
        );
        TencentCosObjectStorage storage = new TencentCosObjectStorage(properties);
        COSClient client = mock(COSClient.class);
        doThrow(new IllegalStateException("COS access denied"))
                .when(client).putObject(any(PutObjectRequest.class));
        setClient(storage, client);

        Logger logger = (Logger) LoggerFactory.getLogger(TencentCosObjectStorage.class);
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);
        try {
            assertThatThrownBy(() -> storage.put("blog/prod/test.png", new byte[] { 1 }, "image/png"))
                    .isInstanceOf(ApiException.class)
                    .hasMessage("COS 上传失败，请稍后重试");

            assertThat(appender.list)
                    .anySatisfy(event -> assertThat(event.getFormattedMessage())
                            .contains("COS upload failed")
                            .contains("IllegalStateException")
                            .contains("COS access denied"));
        } finally {
            logger.detachAppender(appender);
        }
    }

    private static void setClient(TencentCosObjectStorage storage, COSClient client) throws Exception {
        Field field = TencentCosObjectStorage.class.getDeclaredField("client");
        field.setAccessible(true);
        field.set(storage, client);
    }
}
