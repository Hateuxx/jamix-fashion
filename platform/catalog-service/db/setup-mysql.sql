-- One-time MySQL setup for Jamix (run as root / an admin account). The schema itself is NOT
-- created here: Flyway creates the tables from src/main/resources/db/migration when the app starts.
--
-- Don't put real passwords in this file (it's versioned). Instead:
--   1. Copy it to setup-mysql.local.sql (git-ignored) and replace the two CHANGE_ME values there.
--   2. From the catalog-service folder:
--        mysql -u root -p -e "source db/setup-mysql.local.sql"
--   3. Delete setup-mysql.local.sql and put the same passwords in catalog-service/.env.

-- utf8mb4 stores every character (ñ, á, emoji); the 0900 collation compares them sensibly.
CREATE DATABASE IF NOT EXISTS jamix_fashion
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_0900_ai_ci;

-- Least privilege: two accounts.
--   jamix_migrator: changes the structure. Used only by Flyway at startup (DB_MIGRATION_*).
--   jamix_app:      reads and writes data, cannot drop or alter tables (DB_USERNAME / DB_PASSWORD).
-- REQUIRE SSL: the server rejects any unencrypted connection from these accounts.
-- 'localhost' = only from this machine. For a remote app server use its IP or a subnet, never '%'.
CREATE USER IF NOT EXISTS 'jamix_migrator'@'localhost' IDENTIFIED BY 'CHANGE_ME_MIGRATOR_PASSWORD' REQUIRE SSL;
CREATE USER IF NOT EXISTS 'jamix_app'@'localhost'      IDENTIFIED BY 'CHANGE_ME_APP_PASSWORD'      REQUIRE SSL;

GRANT SELECT, INSERT, UPDATE, DELETE, CREATE, ALTER, DROP, INDEX, REFERENCES
    ON jamix_fashion.* TO 'jamix_migrator'@'localhost';

GRANT SELECT, INSERT, UPDATE, DELETE
    ON jamix_fashion.* TO 'jamix_app'@'localhost';

-- Check:
SHOW GRANTS FOR 'jamix_app'@'localhost';
