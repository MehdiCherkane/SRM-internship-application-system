-- ============================================================
-- SRM-BK Internship Portal — Safe fresh install (client)
-- Usage: mysql -u root -p < schema-install.sql
-- ============================================================

CREATE DATABASE IF NOT EXISTS `internship_db`
  DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE `internship_db`;

-- -----------------------------
-- Table: users
-- -----------------------------
CREATE TABLE IF NOT EXISTS `users` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `name` VARCHAR(255) NOT NULL,
  `email` VARCHAR(255) NOT NULL,
  `username` VARCHAR(255) NOT NULL,
  `password` VARCHAR(255) NOT NULL,
  `role` VARCHAR(20) NOT NULL DEFAULT 'APPLICANT',
  `created_at` DATETIME(6) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_users_email` (`email`),
  UNIQUE KEY `uk_users_username` (`username`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- -----------------------------
-- Table: applications
-- -----------------------------
CREATE TABLE IF NOT EXISTS `applications` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `version` BIGINT NULL,
  `first_name` VARCHAR(255) NULL,
  `last_name` VARCHAR(255) NULL,
  `email` VARCHAR(255) NULL,
  `cni` VARCHAR(30) NULL,
  `phone` VARCHAR(255) NULL,
  `university` VARCHAR(255) NULL,
  `major` VARCHAR(255) NULL,
  `cover_message` TEXT NULL,
  `cv_file_path` VARCHAR(255) NULL,
  `submitted_date` DATETIME NULL,
  `cycle_year` INT NULL,
  `status` VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
  `applicant_id` BIGINT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_applications_applicant` (`applicant_id`),
  CONSTRAINT `fk_applications_applicant`
    FOREIGN KEY (`applicant_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- -----------------------------
-- Table: password_reset_tokens
-- -----------------------------
CREATE TABLE IF NOT EXISTS `password_reset_tokens` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `expiry_date` DATETIME(6) NOT NULL,
  `token` VARCHAR(255) NOT NULL,
  `used` BIT(1) NOT NULL DEFAULT 0,
  `user_id` BIGINT NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_reset_token` (`token`),
  KEY `idx_reset_tokens_user` (`user_id`),
  CONSTRAINT `fk_reset_tokens_user`
    FOREIGN KEY (`user_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
