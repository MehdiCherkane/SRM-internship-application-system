-- ============================================================
-- ONEP Internship Portal — Production Schema Migration
-- Run this once on the target database before deploying the JAR.
-- Usage: mysql -u <user> -p internship_db < migration.sql
-- ============================================================

-- 1. Create a limited-privilege application user
--    (replace '<app-password>' with a strong random password)
CREATE USER IF NOT EXISTS 'internship_app'@'localhost'
  IDENTIFIED BY '<app-password>';
GRANT SELECT, INSERT, UPDATE, DELETE ON internship_db.*
  TO 'internship_app'@'localhost';
FLUSH PRIVILEGES;

-- 2. Legacy admins table (unified into users with role=ADMIN)
--    SAFE: commented out by default. Only uncomment if you are sure
--    this table is leftover from the old version and holds no useful data.
-- DROP TABLE IF EXISTS admins;

-- 3. Stale foreign key pointing to admins table
--    MySQL does not support "DROP FOREIGN KEY IF EXISTS".
--    Run the SELECT below first; only run the ALTER if the key exists.
--    SELECT CONSTRAINT_NAME FROM INFORMATION_SCHEMA.TABLE_CONSTRAINTS
--      WHERE TABLE_SCHEMA = 'internship_db'
--        AND TABLE_NAME = 'password_reset_tokens'
--        AND CONSTRAINT_TYPE = 'FOREIGN KEY';
--    Then if needed:
--    ALTER TABLE password_reset_tokens DROP FOREIGN KEY <constraint_name>;

-- 4. Make NOT NULL columns nullable to match JPA entity defaults
--    (draft applications may have null fields)
ALTER TABLE applications
  MODIFY first_name       varchar(255) NULL,
  MODIFY last_name        varchar(255) NULL,
  MODIFY email            varchar(255) NULL,
  MODIFY phone            varchar(255) NULL,
  MODIFY university       varchar(255) NULL,
  MODIFY major            varchar(255) NULL,
  MODIFY submitted_date   datetime     NULL,
  ALTER  status           SET DEFAULT 'DRAFT';

-- 5. Tighten users.created_at to NOT NULL (matches entity)
ALTER TABLE users
  MODIFY created_at datetime(6) NOT NULL;

-- ============================================================
-- Done. You can now start the application with:
--   java -jar internship-0.0.1-SNAPSHOT.jar
-- The UserSeeder will create the initial admin account ONLY if
-- APP_ADMIN_PASSWORD is set (see .env.example).
-- CHANGE THE PASSWORD IMMEDIATELY after first login.
-- ============================================================
