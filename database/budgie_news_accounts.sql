CREATE TABLE IF NOT EXISTS users (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT PRIMARY KEY,
    email VARCHAR(255) NOT NULL UNIQUE,
    display_name VARCHAR(120) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS user_settings (
    user_id BIGINT UNSIGNED NOT NULL PRIMARY KEY,
    uk_location VARCHAR(80) NOT NULL DEFAULT 'United Kingdom',
    default_section VARCHAR(40) NOT NULL DEFAULT 'HEADLINES',
    default_source VARCHAR(40) NOT NULL DEFAULT 'ALL',
    biometric_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    breaking_notifications_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    important_notifications_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    send_app_statistics BOOLEAN NOT NULL DEFAULT TRUE,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_user_settings_user
        FOREIGN KEY (user_id) REFERENCES users(id)
        ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS technical_feedback (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT UNSIGNED NULL,
    app_version VARCHAR(40) NOT NULL,
    message TEXT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_technical_feedback_user
        FOREIGN KEY (user_id) REFERENCES users(id)
        ON DELETE SET NULL
);

-- The Budgie News backend also performs automatic schema healing on startup and before account sync.
-- It creates missing tables, adds missing columns, and restores known indexes/foreign keys where possible.
