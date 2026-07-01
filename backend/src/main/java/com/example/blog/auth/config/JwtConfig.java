package com.example.blog.auth.config;

import java.nio.charset.StandardCharsets;
import java.util.List;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import com.nimbusds.jose.proc.SecurityContext;

@Configuration
@EnableConfigurationProperties(AuthProperties.class)
public class JwtConfig {

    @Bean
    SecretKey jwtSecretKey(AuthProperties properties) {
        byte[] secret = properties.jwtSecret().getBytes(StandardCharsets.UTF_8);
        if (secret.length < 32) {
            throw new IllegalStateException("blog.auth.jwt-secret must contain at least 32 UTF-8 bytes");
        }
        return new SecretKeySpec(secret, "HmacSHA256");
    }

    @Bean
    JwtEncoder jwtEncoder(SecretKey secretKey) {
        return new NimbusJwtEncoder(new ImmutableSecret<SecurityContext>(secretKey));
    }

    @Bean("accessJwtDecoder")
    @Primary
    JwtDecoder accessJwtDecoder(SecretKey secretKey, AuthProperties properties) {
        return decoder(secretKey, properties, "access");
    }

    @Bean("refreshJwtDecoder")
    JwtDecoder refreshJwtDecoder(SecretKey secretKey, AuthProperties properties) {
        return decoder(secretKey, properties, "refresh");
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }

    private JwtDecoder decoder(SecretKey secretKey, AuthProperties properties, String tokenType) {
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withSecretKey(secretKey)
                .macAlgorithm(MacAlgorithm.HS256)
                .build();
        OAuth2TokenValidator<Jwt> issuer = JwtValidators.createDefaultWithIssuer(properties.issuer());
        OAuth2TokenValidator<Jwt> audience = claimValidator(
                "invalid_token",
                "JWT audience is invalid",
                jwt -> jwt.getAudience().contains(properties.audience())
        );
        OAuth2TokenValidator<Jwt> type = claimValidator(
                "invalid_token",
                "JWT token_type is invalid",
                jwt -> tokenType.equals(jwt.getClaimAsString("token_type"))
        );
        decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(List.of(issuer, audience, type)));
        return decoder;
    }

    private OAuth2TokenValidator<Jwt> claimValidator(
            String code,
            String description,
            java.util.function.Predicate<Jwt> predicate
    ) {
        OAuth2Error error = new OAuth2Error(code, description, null);
        return jwt -> predicate.test(jwt)
                ? OAuth2TokenValidatorResult.success()
                : OAuth2TokenValidatorResult.failure(error);
    }
}
