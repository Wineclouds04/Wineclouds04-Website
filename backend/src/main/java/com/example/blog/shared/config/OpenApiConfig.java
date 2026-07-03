package com.example.blog.shared.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springdoc.core.customizers.OpenApiCustomizer;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;

@Configuration
public class OpenApiConfig {

    private static final String BEARER_AUTH = "bearerAuth";

    @Bean
    OpenAPI personalBlogOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Personal Blog API")
                        .description("个人博客公开站与管理后台 HTTP API")
                        .version("0.6.0"))
                .components(new Components()
                        .addSecuritySchemes(BEARER_AUTH, new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("管理员登录后返回的 Access Token")));
    }

    @Bean
    OpenApiCustomizer protectedOperationsCustomizer() {
        return openApi -> openApi.getPaths().forEach((path, pathItem) -> {
            if (path.startsWith("/api/v1/admin/")
                    || path.equals("/api/v1/auth/logout-all")
                    || path.equals("/api/v1/auth/me")
                    || path.equals("/api/v1/auth/password")) {
                pathItem.readOperations().forEach(operation ->
                        operation.addSecurityItem(
                                new SecurityRequirement().addList(BEARER_AUTH)));
            }
        });
    }
}
