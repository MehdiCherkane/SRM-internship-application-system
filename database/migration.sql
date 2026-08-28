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

-- 2. Drop legacy admins table (unified into users with role=ADMIN)
DROP TABLE IF EXISTS admins;

-- 3. Drop stale foreign key pointing to admins table
--    (if it still exists — already removed in dev)
ALTER TABLE password_reset_tokens
  DROP FOREIGN KEY IF EXISTS FKs1hs0cnnlp8l7rhx4cwc4kwi7;

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
-- The UserSeeder will create the default admin account
-- (username=admin, password=admin123) on first boot.
-- CHANGE THE PASSWORD IMMEDIATELY after first login.
-- ============================================================
