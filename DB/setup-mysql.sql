-- MySQL 8.x
SET NAMES utf8mb4;
CREATE DATABASE IF NOT EXISTS smarthelpdeskdb_be
  CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
USE smarthelpdeskdb_be;

CREATE TABLE IF NOT EXISTS `companies` (
  `company_id` varchar(20) NOT NULL,
  `company_name` varchar(200) NOT NULL,
  `address` varchar(500) DEFAULT NULL,
  `phone` varchar(20) DEFAULT NULL,
  PRIMARY KEY (`company_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
CREATE TABLE IF NOT EXISTS `members` (
  `id` char(36) NOT NULL,
  `email` varchar(100) NOT NULL,
  `password` varchar(255) NOT NULL,
  `name` varchar(100) NOT NULL,
  `role` varchar(20) NOT NULL,
  `created_at` timestamp NOT NULL,
  `updated_at` timestamp NOT NULL,
  `created_by` char(36) DEFAULT NULL,
  `updated_by` char(36) DEFAULT NULL,
  `company_id` varchar(20) DEFAULT NULL,
  `phone` varchar(20) DEFAULT NULL,
  `status` varchar(20) DEFAULT 'active',
  `deleted_at` timestamp NULL DEFAULT NULL,
  `is_deleted` bit(1) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `email` (`email`),
  KEY `fk_members_companies` (`company_id`),
  CONSTRAINT `fk_members_companies` FOREIGN KEY (`company_id`) REFERENCES `companies` (`company_id`) ON DELETE SET NULL ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
CREATE TABLE IF NOT EXISTS `requests` (
  `id` char(36) NOT NULL,
  `company_id` varchar(20) NOT NULL,
  `title` varchar(255) NOT NULL,
  `description` longtext,
  `category` varchar(20) NOT NULL,
  `priority` varchar(20) NOT NULL DEFAULT 'MEDIUM',
  `status` varchar(20) NOT NULL DEFAULT 'PENDING',
  `client_id` char(36) NOT NULL,
  `assigned_developer_id` char(36) DEFAULT NULL,
  `created_at` timestamp NOT NULL,
  `updated_at` timestamp NOT NULL,
  `created_by` char(36) DEFAULT NULL,
  `updated_by` char(36) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `fk_requests_companies` (`company_id`),
  KEY `fk_requests_client` (`client_id`),
  KEY `fk_requests_developer` (`assigned_developer_id`),
  CONSTRAINT `fk_requests_client` FOREIGN KEY (`client_id`) REFERENCES `members` (`id`),
  CONSTRAINT `fk_requests_companies` FOREIGN KEY (`company_id`) REFERENCES `companies` (`company_id`) ON UPDATE CASCADE,
  CONSTRAINT `fk_requests_developer` FOREIGN KEY (`assigned_developer_id`) REFERENCES `members` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
CREATE TABLE IF NOT EXISTS `request_histories` (
  `id` char(36) NOT NULL,
  `request_id` char(36) NOT NULL,
  `changed_by` char(36) NOT NULL,
  `action` varchar(20) NOT NULL,
  `from_status` varchar(20) DEFAULT NULL,
  `to_status` varchar(20) DEFAULT NULL,
  `memo` longtext,
  `changed_at` timestamp NOT NULL,
  PRIMARY KEY (`id`),
  KEY `fk_histories_requests` (`request_id`),
  KEY `fk_histories_members` (`changed_by`),
  CONSTRAINT `fk_histories_members` FOREIGN KEY (`changed_by`) REFERENCES `members` (`id`),
  CONSTRAINT `fk_histories_requests` FOREIGN KEY (`request_id`) REFERENCES `requests` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
CREATE TABLE IF NOT EXISTS `alerts` (
  `id` char(36) NOT NULL,
  `request_id` char(36) NOT NULL,
  `target_member_id` char(36) NOT NULL,
  `alert_type` varchar(30) NOT NULL,
  `message` longtext NOT NULL,
  `is_read` bit(1) NOT NULL,
  `created_at` timestamp NOT NULL,
  PRIMARY KEY (`id`),
  KEY `fk_alerts_requests` (`request_id`),
  KEY `fk_alerts_members` (`target_member_id`),
  CONSTRAINT `fk_alerts_members` FOREIGN KEY (`target_member_id`) REFERENCES `members` (`id`),
  CONSTRAINT `fk_alerts_requests` FOREIGN KEY (`request_id`) REFERENCES `requests` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
CREATE TABLE IF NOT EXISTS `chat_messages` (
  `id` char(36) NOT NULL,
  `request_id` char(36) NOT NULL,
  `sender_id` char(36) NOT NULL,
  `message` longtext NOT NULL,
  `message_type` varchar(20) NOT NULL DEFAULT 'TEXT',
  `is_read` bit(1) NOT NULL,
  `created_at` timestamp NOT NULL,
  PRIMARY KEY (`id`),
  KEY `fk_chat_messages_requests` (`request_id`),
  KEY `fk_chat_messages_members` (`sender_id`),
  CONSTRAINT `fk_chat_messages_members` FOREIGN KEY (`sender_id`) REFERENCES `members` (`id`),
  CONSTRAINT `fk_chat_messages_requests` FOREIGN KEY (`request_id`) REFERENCES `requests` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;




-- thêm dữ liệu công ty mặc định
INSERT INTO companies (company_id, company_name, address, phone)
SELECT 'KR_SAMSUNG', 'Samsung C&T Corporation', '67 Seochodae-ro, Seocho-gu, Seoul, South Korea', '+82-2145-1114'

INSERT INTO companies (company_id, company_name, address, phone)
SELECT 'KR_NAVER', 'Naver Financial Corp.', '6 Buljeong-ro, Bundang-gu, Seongnam-si, Gyeonggi-do, South Korea', '+82-1588-3820'

INSERT INTO companies (company_id, company_name, address, phone)
SELECT 'KR_KAKAO', 'Kakao Mobility Corp.', '242 Pangyoyeok-ro, Bundang-gu, Seongnam-si, Gyeonggi-do, South Korea', '+82-1599-9400'

INSERT INTO companies (company_id, company_name, address, phone)
SELECT 'BZCOM', 'BZCOM Technology Corp.', '120 Teheran-ro, Gangnam-gu, Seoul, South Korea', '+82-2555-0199'

SHOW TABLES;
SELECT company_id, company_name, address, phone FROM companies
WHERE company_id IN ('KR_CLIENT_Ss', 'KR_CLIENT_Nv', 'KR_CLIENT_Kk');
