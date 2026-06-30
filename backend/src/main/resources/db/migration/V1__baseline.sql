CREATE TABLE sys_user (
    id BIGINT NOT NULL AUTO_INCREMENT,
    username VARCHAR(64) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    nickname VARCHAR(64) NOT NULL,
    email VARCHAR(160) NULL,
    role VARCHAR(32) NOT NULL,
    status VARCHAR(32) NOT NULL,
    last_login_at DATETIME(3) NULL,
    password_changed_at DATETIME(3) NULL,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    version INT NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_sys_user_username (username),
    UNIQUE KEY uk_sys_user_email (email),
    KEY idx_sys_user_status (status)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE category (
    id BIGINT NOT NULL AUTO_INCREMENT,
    name VARCHAR(64) NOT NULL,
    slug VARCHAR(80) NOT NULL,
    description VARCHAR(300) NULL,
    sort_order INT NOT NULL DEFAULT 0,
    visible TINYINT NOT NULL DEFAULT 1,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    UNIQUE KEY uk_category_name (name),
    UNIQUE KEY uk_category_slug (slug),
    KEY idx_category_visible_sort (visible, sort_order)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE tag (
    id BIGINT NOT NULL AUTO_INCREMENT,
    name VARCHAR(64) NOT NULL,
    slug VARCHAR(80) NOT NULL,
    description VARCHAR(300) NULL,
    sort_order INT NOT NULL DEFAULT 0,
    visible TINYINT NOT NULL DEFAULT 1,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    UNIQUE KEY uk_tag_name (name),
    UNIQUE KEY uk_tag_slug (slug),
    KEY idx_tag_visible_sort (visible, sort_order)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE media_asset (
    id BIGINT NOT NULL AUTO_INCREMENT,
    object_key VARCHAR(500) NOT NULL,
    original_name VARCHAR(255) NOT NULL,
    media_type VARCHAR(100) NOT NULL,
    extension VARCHAR(20) NULL,
    size_bytes BIGINT NOT NULL,
    width INT NULL,
    height INT NULL,
    sha256 CHAR(64) NOT NULL,
    alt_text VARCHAR(300) NULL,
    status VARCHAR(32) NOT NULL,
    reference_count INT NOT NULL DEFAULT 0,
    created_by BIGINT NULL,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    deleted_at DATETIME(3) NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_media_asset_object_key (object_key),
    KEY idx_media_asset_status_created (status, created_at),
    KEY idx_media_asset_sha256 (sha256),
    CONSTRAINT fk_media_asset_created_by
        FOREIGN KEY (created_by) REFERENCES sys_user (id) ON DELETE SET NULL
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE article (
    id BIGINT NOT NULL AUTO_INCREMENT,
    title VARCHAR(200) NOT NULL,
    slug VARCHAR(160) NOT NULL,
    summary VARCHAR(600) NULL,
    content_markdown LONGTEXT NOT NULL,
    content_html LONGTEXT NOT NULL,
    content_plain LONGTEXT NOT NULL,
    cover_media_id BIGINT NULL,
    category_id BIGINT NULL,
    status VARCHAR(32) NOT NULL,
    visibility VARCHAR(32) NOT NULL DEFAULT 'PUBLIC',
    is_pinned TINYINT NOT NULL DEFAULT 0,
    allow_comment TINYINT NOT NULL DEFAULT 1,
    word_count INT NOT NULL DEFAULT 0,
    reading_minutes INT NOT NULL DEFAULT 0,
    view_count BIGINT NOT NULL DEFAULT 0,
    like_count BIGINT NOT NULL DEFAULT 0,
    comment_count BIGINT NOT NULL DEFAULT 0,
    meta_title VARCHAR(200) NULL,
    meta_description VARCHAR(320) NULL,
    canonical_url VARCHAR(500) NULL,
    published_at DATETIME(3) NULL,
    scheduled_at DATETIME(3) NULL,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    deleted_at DATETIME(3) NULL,
    version INT NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_article_slug (slug),
    KEY idx_article_status_publish (status, published_at DESC),
    KEY idx_article_category_status (category_id, status, published_at DESC),
    KEY idx_article_pinned_publish (is_pinned, published_at DESC),
    KEY idx_article_cover_media (cover_media_id),
    FULLTEXT KEY idx_article_fulltext (title, summary, content_plain) WITH PARSER ngram,
    CONSTRAINT fk_article_cover_media
        FOREIGN KEY (cover_media_id) REFERENCES media_asset (id) ON DELETE SET NULL,
    CONSTRAINT fk_article_category
        FOREIGN KEY (category_id) REFERENCES category (id) ON DELETE SET NULL
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE article_tag (
    article_id BIGINT NOT NULL,
    tag_id BIGINT NOT NULL,
    PRIMARY KEY (article_id, tag_id),
    KEY idx_article_tag_tag (tag_id, article_id),
    CONSTRAINT fk_article_tag_article
        FOREIGN KEY (article_id) REFERENCES article (id) ON DELETE CASCADE,
    CONSTRAINT fk_article_tag_tag
        FOREIGN KEY (tag_id) REFERENCES tag (id) ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE comment (
    id BIGINT NOT NULL AUTO_INCREMENT,
    article_id BIGINT NULL,
    root_id BIGINT NULL,
    parent_id BIGINT NULL,
    type VARCHAR(32) NOT NULL,
    content_markdown TEXT NOT NULL,
    content_html TEXT NOT NULL,
    nickname VARCHAR(64) NOT NULL,
    email_ciphertext VARBINARY(1024) NULL,
    website VARCHAR(300) NULL,
    anonymous_key_hash CHAR(64) NOT NULL,
    status VARCHAR(32) NOT NULL,
    is_admin_reply TINYINT NOT NULL DEFAULT 0,
    notify_on_reply TINYINT NOT NULL DEFAULT 0,
    ip_hash CHAR(64) NOT NULL,
    user_agent_summary VARCHAR(200) NULL,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    deleted_at DATETIME(3) NULL,
    PRIMARY KEY (id),
    KEY idx_comment_article_status_created (article_id, status, created_at),
    KEY idx_comment_type_status_created (type, status, created_at),
    KEY idx_comment_root_created (root_id, created_at),
    KEY idx_comment_parent (parent_id),
    CONSTRAINT fk_comment_article
        FOREIGN KEY (article_id) REFERENCES article (id) ON DELETE CASCADE,
    CONSTRAINT fk_comment_root
        FOREIGN KEY (root_id) REFERENCES comment (id) ON DELETE CASCADE,
    CONSTRAINT fk_comment_parent
        FOREIGN KEY (parent_id) REFERENCES comment (id) ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE article_like (
    id BIGINT NOT NULL AUTO_INCREMENT,
    article_id BIGINT NOT NULL,
    anonymous_key_hash CHAR(64) NOT NULL,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    UNIQUE KEY uk_article_like_visitor (article_id, anonymous_key_hash),
    KEY idx_article_like_created (created_at),
    CONSTRAINT fk_article_like_article
        FOREIGN KEY (article_id) REFERENCES article (id) ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE article_view_daily (
    article_id BIGINT NOT NULL,
    stat_date DATE NOT NULL,
    view_count BIGINT NOT NULL DEFAULT 0,
    unique_count BIGINT NOT NULL DEFAULT 0,
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (article_id, stat_date),
    KEY idx_article_view_daily_date (stat_date),
    CONSTRAINT fk_article_view_daily_article
        FOREIGN KEY (article_id) REFERENCES article (id) ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE site_visit_daily (
    stat_date DATE NOT NULL,
    view_count BIGINT NOT NULL DEFAULT 0,
    unique_count BIGINT NOT NULL DEFAULT 0,
    direct_count BIGINT NOT NULL DEFAULT 0,
    search_count BIGINT NOT NULL DEFAULT 0,
    referral_count BIGINT NOT NULL DEFAULT 0,
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (stat_date)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE operation_log (
    id BIGINT NOT NULL AUTO_INCREMENT,
    operator_id BIGINT NULL,
    module VARCHAR(64) NOT NULL,
    action VARCHAR(64) NOT NULL,
    target_id VARCHAR(128) NULL,
    result VARCHAR(32) NOT NULL,
    detail_json JSON NULL,
    trace_id VARCHAR(64) NULL,
    ip_hash CHAR(64) NULL,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    KEY idx_operation_log_operator_created (operator_id, created_at),
    KEY idx_operation_log_module_created (module, created_at),
    KEY idx_operation_log_trace_id (trace_id),
    CONSTRAINT fk_operation_log_operator
        FOREIGN KEY (operator_id) REFERENCES sys_user (id) ON DELETE SET NULL
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE site_config (
    id BIGINT NOT NULL AUTO_INCREMENT,
    config_key VARCHAR(120) NOT NULL,
    json_value JSON NOT NULL,
    data_type VARCHAR(32) NOT NULL,
    is_public TINYINT NOT NULL DEFAULT 0,
    description VARCHAR(300) NULL,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    version INT NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_site_config_key (config_key),
    KEY idx_site_config_public (is_public)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE url_redirect (
    id BIGINT NOT NULL AUTO_INCREMENT,
    old_slug VARCHAR(160) NOT NULL,
    target_path VARCHAR(500) NOT NULL,
    http_status SMALLINT NOT NULL DEFAULT 301,
    enabled TINYINT NOT NULL DEFAULT 1,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    UNIQUE KEY uk_url_redirect_old_slug (old_slug),
    KEY idx_url_redirect_enabled (enabled)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE notification_outbox (
    id BIGINT NOT NULL AUTO_INCREMENT,
    event_type VARCHAR(64) NOT NULL,
    recipient_ciphertext VARBINARY(1024) NOT NULL,
    template_name VARCHAR(120) NOT NULL,
    payload_json JSON NOT NULL,
    status VARCHAR(32) NOT NULL,
    retry_count INT NOT NULL DEFAULT 0,
    next_attempt_at DATETIME(3) NOT NULL,
    last_error VARCHAR(1000) NULL,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    KEY idx_notification_outbox_pending (status, next_attempt_at),
    KEY idx_notification_outbox_created (created_at)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;
