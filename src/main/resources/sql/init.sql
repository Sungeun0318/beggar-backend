-- 거지 우정 수호대 로컬 MySQL 초기화 스크립트
-- 실행: mysql -u root -p1208 < src/main/resources/sql/init.sql
-- 샘플 데이터: mysql -u root -p1208 beggar < src/main/resources/sql/data.sql

CREATE DATABASE IF NOT EXISTS beggar
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;

USE beggar;

-- 1. users
CREATE TABLE IF NOT EXISTS users (
  user_no            BIGINT       NOT NULL AUTO_INCREMENT,
  user_name          VARCHAR(15)  NOT NULL,
  password_hash      VARCHAR(255) NULL,
  profile_image_url  VARCHAR(500) NULL,
  uemail             VARCHAR(100) NOT NULL,
  role               VARCHAR(20)  NOT NULL DEFAULT 'USER',
  gender             INT          NULL,
  age_range          VARCHAR(20)  NULL,
  created_at         DATETIME     NOT NULL,
  updated_at         DATETIME     NOT NULL,
  PRIMARY KEY (user_no),
  UNIQUE KEY uk_users_user_name (user_name),
  UNIQUE KEY uk_users_uemail (uemail)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 2. rooms
CREATE TABLE IF NOT EXISTS rooms (
  room_no           BIGINT       NOT NULL AUTO_INCREMENT,
  owner_user_no     BIGINT       NOT NULL,
  room_name         VARCHAR(15)  NOT NULL,
  room_code         VARCHAR(15)  NOT NULL,
  max_member_count  INT          NOT NULL DEFAULT 100,
  total_budget      INT          NULL,
  is_friends        BOOLEAN      NOT NULL,
  location          VARCHAR(255) NULL,
  status            VARCHAR(20)  NOT NULL DEFAULT 'INVITING',
  room_created      DATETIME     NOT NULL,
  ended_at          DATETIME     NULL,
  deleted_at        DATETIME     NULL,
  PRIMARY KEY (room_no),
  UNIQUE KEY uk_rooms_room_name (room_name),
  UNIQUE KEY uk_rooms_room_code (room_code),
  CONSTRAINT fk_rooms_owner FOREIGN KEY (owner_user_no) REFERENCES users(user_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

ALTER TABLE rooms
  ADD COLUMN IF NOT EXISTS location VARCHAR(255) NULL;

ALTER TABLE rooms
  ADD COLUMN IF NOT EXISTS status VARCHAR(20) NOT NULL DEFAULT 'INVITING';

ALTER TABLE rooms
  ADD COLUMN IF NOT EXISTS ended_at DATETIME NULL;

ALTER TABLE rooms
  ADD COLUMN IF NOT EXISTS deleted_at DATETIME NULL;

-- 3. room_members
CREATE TABLE IF NOT EXISTS room_members (
  room_member_id  BIGINT       NOT NULL AUTO_INCREMENT,
  room_no         BIGINT       NOT NULL,
  user_no         BIGINT       NOT NULL,
  status          VARCHAR(15)  NOT NULL,
  is_hidden       BOOLEAN      NOT NULL DEFAULT FALSE,
  joined_at       DATETIME     NOT NULL,
  left_at         DATETIME     NULL,
  PRIMARY KEY (room_member_id),
  UNIQUE KEY uk_room_members_room_user (room_no, user_no),
  CONSTRAINT fk_room_members_room FOREIGN KEY (room_no) REFERENCES rooms(room_no),
  CONSTRAINT fk_room_members_user FOREIGN KEY (user_no) REFERENCES users(user_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

ALTER TABLE room_members
  ADD COLUMN IF NOT EXISTS is_hidden BOOLEAN NOT NULL DEFAULT FALSE;

-- 4. room_purpose_tags
CREATE TABLE IF NOT EXISTS room_purpose_tags (
  tag_id    BIGINT       NOT NULL AUTO_INCREMENT,
  room_no   BIGINT       NOT NULL,
  tag_tags  VARCHAR(30)  NOT NULL,
  PRIMARY KEY (tag_id),
  CONSTRAINT fk_tags_room FOREIGN KEY (room_no) REFERENCES rooms(room_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 5. budgets
CREATE TABLE IF NOT EXISTS budgets (
  budget_id       BIGINT    NOT NULL AUTO_INCREMENT,
  room_member_id  BIGINT    NOT NULL,
  budget_amount   INT       NOT NULL,
  submitted_at    DATETIME  NOT NULL,
  PRIMARY KEY (budget_id),
  UNIQUE KEY uk_budgets_member (room_member_id),
  CONSTRAINT fk_budgets_member FOREIGN KEY (room_member_id) REFERENCES room_members(room_member_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 6. room_budget_results
CREATE TABLE IF NOT EXISTS room_budget_results (
  result_id              BIGINT    NOT NULL AUTO_INCREMENT,
  room_no                BIGINT    NOT NULL,
  min_budget_per_person  INT       NOT NULL,
  member_count           INT       NOT NULL,
  total_budget           INT       NOT NULL,
  confirmed_at           DATETIME  NOT NULL,
  PRIMARY KEY (result_id),
  UNIQUE KEY uk_results_room (room_no),
  CONSTRAINT fk_results_room FOREIGN KEY (room_no) REFERENCES rooms(room_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 7. receipt_split_groups
CREATE TABLE IF NOT EXISTS receipt_split_groups (
  split_group_id            BIGINT        NOT NULL AUTO_INCREMENT,
  room_no                   BIGINT        NOT NULL,
  store_name                VARCHAR(150)  NOT NULL,
  address                   VARCHAR(200)  NOT NULL,
  center_lat                DECIMAL(10,7) NULL,
  center_lng                DECIMAL(10,7) NULL,
  status                    VARCHAR(20)   NOT NULL DEFAULT 'OPEN',
  created_by_room_member_id BIGINT        NOT NULL,
  closed_at                 DATETIME      NULL,
  created_at                DATETIME      NOT NULL,
  updated_at                DATETIME      NOT NULL,
  PRIMARY KEY (split_group_id),
  INDEX idx_split_groups_room_status (room_no, status),
  CONSTRAINT fk_receipt_split_groups_room FOREIGN KEY (room_no) REFERENCES rooms(room_no),
  CONSTRAINT fk_receipt_split_groups_created_by FOREIGN KEY (created_by_room_member_id) REFERENCES room_members(room_member_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 8. receipts
CREATE TABLE IF NOT EXISTS receipts (
  receipt_id                 BIGINT        NOT NULL AUTO_INCREMENT,
  room_no                    BIGINT        NOT NULL,
  room_member_id             BIGINT        NOT NULL,
  receipt_type               VARCHAR(20)   NOT NULL,
  input_method               VARCHAR(20)   NOT NULL,
  image_url                  VARCHAR(500)  NULL,
  image_hash                 VARCHAR(64)   NULL,
  ocr_status                 VARCHAR(30)   NOT NULL,
  store_name                 VARCHAR(150)  NULL,
  total_amount               INT           NULL,
  amount                     INT           NOT NULL,
  receipt_issued_at          DATETIME      NULL,
  address                    VARCHAR(100)  NULL,
  center_lat                 DECIMAL(10,7) NULL,
  center_lng                 DECIMAL(10,7) NULL,
  split_group_id             BIGINT        NULL,
  good_price_store_id        VARCHAR(100)  NULL,
  good_price_store_name      VARCHAR(150)  NULL,
  good_price_store_address   VARCHAR(200)  NULL,
  good_price_matched         BOOLEAN       NOT NULL DEFAULT FALSE,
  good_price_verified_at     DATETIME      NULL,
  confirmed                  BOOLEAN       NOT NULL DEFAULT TRUE,
  created_at                 DATETIME      NOT NULL,
  updated_at                 DATETIME      NOT NULL,
  PRIMARY KEY (receipt_id),
  CONSTRAINT fk_receipts_room FOREIGN KEY (room_no) REFERENCES rooms(room_no),
  CONSTRAINT fk_receipts_member FOREIGN KEY (room_member_id) REFERENCES room_members(room_member_id),
  CONSTRAINT fk_receipts_split_group FOREIGN KEY (split_group_id) REFERENCES receipt_split_groups(split_group_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

ALTER TABLE receipts
  ADD COLUMN IF NOT EXISTS split_group_id BIGINT NULL;

ALTER TABLE receipts
  ADD COLUMN IF NOT EXISTS receipt_issued_at DATETIME NULL;

ALTER TABLE receipts
  ADD COLUMN IF NOT EXISTS image_hash VARCHAR(64) NULL;

ALTER TABLE receipts
  ADD COLUMN IF NOT EXISTS confirmed BOOLEAN NOT NULL DEFAULT TRUE;

-- 9. receipt_splits
CREATE TABLE IF NOT EXISTS receipt_splits (
  split_id        BIGINT    NOT NULL AUTO_INCREMENT,
  receipt_id      BIGINT    NOT NULL,
  room_member_id  BIGINT    NOT NULL,
  amount          INT       NOT NULL,
  created_at      DATETIME  NOT NULL,
  updated_at      DATETIME  NOT NULL,
  PRIMARY KEY (split_id),
  UNIQUE KEY uk_receipt_splits_receipt_member (receipt_id, room_member_id),
  CONSTRAINT fk_receipt_splits_receipt FOREIGN KEY (receipt_id) REFERENCES receipts(receipt_id),
  CONSTRAINT fk_receipt_splits_member FOREIGN KEY (room_member_id) REFERENCES room_members(room_member_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 10. room_beggar_scores
CREATE TABLE IF NOT EXISTS room_beggar_scores (
  score_id                   BIGINT        NOT NULL AUTO_INCREMENT,
  room_no                    BIGINT        NOT NULL,
  score                      INT           NOT NULL DEFAULT 0,
  title                      VARCHAR(30)   NOT NULL DEFAULT '아기 거지',
  total_spent_amount         BIGINT        NOT NULL DEFAULT 0,
  total_saved_amount         BIGINT        NOT NULL DEFAULT 0,
  good_price_verified_count  INT           NOT NULL DEFAULT 0,
  budget_compliance_rate     DECIMAL(5,2)  NULL,
  avg_savings_ratio          DECIMAL(5,2)  NULL,
  last_calculated_at         DATETIME      NOT NULL,
  updated_at                 DATETIME      NOT NULL,
  PRIMARY KEY (score_id),
  UNIQUE KEY uk_room_beggar_scores_room (room_no),
  INDEX idx_room_scores_score_desc (score DESC),
  CONSTRAINT fk_room_beggar_scores_room FOREIGN KEY (room_no) REFERENCES rooms(room_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 11. community_posts
CREATE TABLE IF NOT EXISTS community_posts (
  post_id     BIGINT       NOT NULL AUTO_INCREMENT,
  user_no     BIGINT       NOT NULL,
  title       VARCHAR(100) NOT NULL,
  content     TEXT         NOT NULL,
  category    VARCHAR(30)  NULL,
  created_at  DATETIME     NOT NULL,
  updated_at  DATETIME     NOT NULL,
  PRIMARY KEY (post_id),
  INDEX idx_community_posts_created_at (created_at DESC),
  CONSTRAINT fk_community_posts_user FOREIGN KEY (user_no) REFERENCES users(user_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 12. room_free_posts
CREATE TABLE IF NOT EXISTS room_free_posts (
  post_id     BIGINT       NOT NULL AUTO_INCREMENT,
  user_no     BIGINT       NOT NULL,
  title       VARCHAR(255) NOT NULL,
  content     TEXT         NOT NULL,
  created_at  DATETIME     NOT NULL,
  updated_at  DATETIME     NOT NULL,
  PRIMARY KEY (post_id),
  INDEX idx_rf_post_created (created_at DESC),
  CONSTRAINT fk_rf_posts_user FOREIGN KEY (user_no) REFERENCES users(user_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 13. room_free_comments
CREATE TABLE IF NOT EXISTS room_free_comments (
  comment_id  BIGINT    NOT NULL AUTO_INCREMENT,
  post_id     BIGINT    NOT NULL,
  user_no     BIGINT    NOT NULL,
  content     TEXT      NOT NULL,
  created_at  DATETIME  NOT NULL,
  PRIMARY KEY (comment_id),
  CONSTRAINT fk_rf_comments_post FOREIGN KEY (post_id) REFERENCES room_free_posts(post_id) ON DELETE CASCADE,
  CONSTRAINT fk_rf_comments_user FOREIGN KEY (user_no) REFERENCES users(user_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 14. room_free_chats
CREATE TABLE IF NOT EXISTS room_free_chats (
  chat_id     BIGINT    NOT NULL AUTO_INCREMENT,
  user_no     BIGINT    NOT NULL,
  message     TEXT      NOT NULL,
  created_at  DATETIME  NOT NULL,
  PRIMARY KEY (chat_id),
  INDEX idx_rf_chat_created (created_at DESC),
  CONSTRAINT fk_rf_chats_user FOREIGN KEY (user_no) REFERENCES users(user_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 15. admin_accounts
CREATE TABLE IF NOT EXISTS admin_accounts (
  id        BIGINT       NOT NULL AUTO_INCREMENT,
  username  VARCHAR(255) NOT NULL,
  password  VARCHAR(255) NOT NULL,
  role      VARCHAR(255) NOT NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_admin_accounts_username (username)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 관리자 계정 1개 (username: admin / password: admin1234)
INSERT INTO admin_accounts (username, password, role)
SELECT 'admin', '$2a$10$iVf4bvDymCqW1T30uuRhIeY9hVKCoyHtlO.PB6oWkL.d7uFWJDv5C', 'ADMIN'
WHERE NOT EXISTS (SELECT 1 FROM admin_accounts WHERE username = 'admin');

-- 16. recommendation_interactions
CREATE TABLE IF NOT EXISTS recommendation_interactions (
  interaction_id   BIGINT       NOT NULL AUTO_INCREMENT,
  user_no          BIGINT       NOT NULL,
  room_no          BIGINT       NOT NULL,
  store_id         VARCHAR(100) NULL,
  store_name       VARCHAR(150) NOT NULL,
  action           VARCHAR(20)  NOT NULL,
  requested_tag    VARCHAR(30)  NULL,
  requested_region VARCHAR(100) NULL,
  rank_position    INT          NULL,
  expected_price   INT          NULL,
  created_at       DATETIME     NOT NULL,
  updated_at       DATETIME     NOT NULL,
  PRIMARY KEY (interaction_id),
  INDEX idx_rec_interactions_user (user_no),
  INDEX idx_rec_interactions_room (room_no),
  INDEX idx_rec_interactions_store (store_id),
  CONSTRAINT fk_rec_interactions_user FOREIGN KEY (user_no) REFERENCES users(user_no),
  CONSTRAINT fk_rec_interactions_room FOREIGN KEY (room_no) REFERENCES rooms(room_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
