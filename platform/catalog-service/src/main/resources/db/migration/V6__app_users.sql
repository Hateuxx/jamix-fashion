-- One account per person (staff and customers) instead of shared credentials in configuration.
-- The first accounts are created at startup from jamix.security.users when this table is empty
-- (see UserBootstrap); from then on the owner manages the team from the admin panel.

CREATE TABLE app_users (
    id                   BIGINT       NOT NULL AUTO_INCREMENT,
    username             VARCHAR(40)  NOT NULL,
    display_name         VARCHAR(80)  NOT NULL,
    password_hash        VARCHAR(255) NOT NULL,
    role                 VARCHAR(16)  NOT NULL,
    active               BOOLEAN      NOT NULL DEFAULT TRUE,
    -- Set when the owner creates the account or resets its password: the person must choose their own.
    must_change_password BOOLEAN      NOT NULL DEFAULT FALSE,
    -- Copied into each token; bumped on password change, reset, deactivation or role change, which
    -- invalidates every token issued before (they carry the old number).
    session_version      INT          NOT NULL DEFAULT 0,
    created_at           DATETIME(6)  NOT NULL,
    version              BIGINT       NOT NULL DEFAULT 0,
    CONSTRAINT pk_app_users PRIMARY KEY (id),
    CONSTRAINT uk_app_users_username UNIQUE (username)
);
