package com.example.blog.auth.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.example.blog.auth.mapper.AuthMapper;

@Component
public class InitialAdminBootstrap implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(InitialAdminBootstrap.class);

    private final AuthMapper authMapper;
    private final PasswordEncoder passwordEncoder;
    private final AuthProperties properties;

    public InitialAdminBootstrap(
            AuthMapper authMapper,
            PasswordEncoder passwordEncoder,
            AuthProperties properties
    ) {
        this.authMapper = authMapper;
        this.passwordEncoder = passwordEncoder;
        this.properties = properties;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        AuthProperties.InitialAdmin admin = properties.initialAdmin();
        if (admin == null
                || !StringUtils.hasText(admin.username())
                || !StringUtils.hasText(admin.password())) {
            return;
        }
        if (authMapper.countUsers() > 0) {
            return;
        }
        if (admin.password().length() < 12 || admin.password().startsWith("change-")) {
            throw new IllegalStateException(
                    "ADMIN_INITIAL_PASSWORD must be changed and contain at least 12 characters"
            );
        }
        String nickname = StringUtils.hasText(admin.nickname()) ? admin.nickname() : admin.username();
        authMapper.insertAdmin(
                admin.username().trim(),
                passwordEncoder.encode(admin.password()),
                nickname.trim()
        );
        log.info("Created initial administrator account '{}'; remove bootstrap credentials now",
                admin.username().trim());
    }
}
