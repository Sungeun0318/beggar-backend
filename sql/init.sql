-- 거지 우정 수호대 — 로컬 MySQL 초기화 스크립트
-- 사용법: mysql -u root -p < sql/init.sql
-- 또는 MySQL Workbench에서 통째로 실행

CREATE DATABASE IF NOT EXISTS beggar
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

USE beggar;

-- ─── 8개 테이블 DDL (docs/DB_DESIGN.md 와 동일) ──────────────────────────────

-- 1. users
CREATE TABLE IF NOT EXISTS users (
  user_no            INT          NOT NULL AUTO_INCREMENT,
  user_name          VARCHAR(15)  NOT NULL,
  password_hash      VARCHAR(255) NULL,
  profile_image_url  VARCHAR(500) NULL,
  uemail             VARCHAR(100) NOT NULL,
  role               VARCHAR(20)  NOT NULL DEFAULT 'USER',
  created_at         DATETIME     NOT NULL,
  updated_at         DATETIME     NOT NULL,
  PRIMARY KEY (user_no),
  UNIQUE KEY uk_users_user_name (user_name),
  UNIQUE KEY uk_users_uemail (uemail)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 2. rooms
CREATE TABLE IF NOT EXISTS rooms (
  room_no       INT          NOT NULL AUTO_INCREMENT,
  user_no       INT          NOT NULL,
  room_name     VARCHAR(15)  NOT NULL,
  room_code     VARCHAR(15)  NOT NULL,
  total_budget  INT          NULL,
  room_created  DATETIME     NOT NULL,
  is_friends    boolean      NOT NULL,
  PRIMARY KEY (room_no),
  UNIQUE KEY uk_rooms_room_name (room_name),
  UNIQUE KEY uk_rooms_room_code (room_code),
  CONSTRAINT fk_rooms_user FOREIGN KEY (user_no) REFERENCES users(user_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 3. room_members
CREATE TABLE IF NOT EXISTS room_members (
  room_member_id  INT          NOT NULL AUTO_INCREMENT,
  room_no         INT          NOT NULL,
  user_no         INT          NOT NULL,
  status          VARCHAR(15)  NOT NULL,
  joined_at       DATETIME     NOT NULL,
  left_at         DATETIME     NULL,
  PRIMARY KEY (room_member_id),
  UNIQUE KEY uk_room_members_room_user (room_no, user_no),
  CONSTRAINT fk_room_members_room FOREIGN KEY (room_no) REFERENCES rooms(room_no),
  CONSTRAINT fk_room_members_user FOREIGN KEY (user_no) REFERENCES users(user_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 4. room_purpose_tags
CREATE TABLE IF NOT EXISTS room_purpose_tags (
  tag_id    INT          NOT NULL AUTO_INCREMENT,
  room_no   INT          NOT NULL,
  tag_tags  VARCHAR(30)  NOT NULL,
  PRIMARY KEY (tag_id),
  CONSTRAINT fk_tags_room FOREIGN KEY (room_no) REFERENCES rooms(room_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 5. budgets
CREATE TABLE IF NOT EXISTS budgets (
  budget_id       INT       NOT NULL AUTO_INCREMENT,
  room_member_id  INT       NOT NULL,
  budget_amount   INT       NOT NULL,
  submitted_at    DATETIME  NOT NULL,
  PRIMARY KEY (budget_id),
  UNIQUE KEY uk_budgets_member (room_member_id),
  CONSTRAINT fk_budgets_member FOREIGN KEY (room_member_id) REFERENCES room_members(room_member_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 6. room_budget_results
CREATE TABLE IF NOT EXISTS room_budget_results (
  result_id              INT       NOT NULL AUTO_INCREMENT,
  room_no                INT       NOT NULL,
  min_budget_per_person  INT       NOT NULL,
  member_count           INT       NOT NULL,
  total_budget           INT       NOT NULL,
  confirmed_at           DATETIME  NOT NULL,
  PRIMARY KEY (result_id),
  UNIQUE KEY uk_results_room (room_no),
  CONSTRAINT fk_results_room FOREIGN KEY (room_no) REFERENCES rooms(room_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 7. receipts
CREATE TABLE IF NOT EXISTS receipts (
  receipt_id      INT            NOT NULL AUTO_INCREMENT,
  room_no         INT            NOT NULL,
  room_member_id  INT            NOT NULL,
  image_url       VARCHAR(500)   NOT NULL,
  ocr_status      VARCHAR(30)    NOT NULL,
  store_name      VARCHAR(150)   NULL,
  total_amount    INT            NULL,
  amount          INT            NOT NULL,
  address         VARCHAR(100)   NULL,
  center_lat      DECIMAL(10,7)  NULL,
  center_lng      DECIMAL(10,7)  NULL,
  created_at      DATETIME       NOT NULL,
  updated_at      DATETIME       NOT NULL,
  PRIMARY KEY (receipt_id),
  CONSTRAINT fk_receipts_room   FOREIGN KEY (room_no)        REFERENCES rooms(room_no),
  CONSTRAINT fk_receipts_member FOREIGN KEY (room_member_id) REFERENCES room_members(room_member_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 8. user_beggar_scores
CREATE TABLE IF NOT EXISTS user_beggar_scores (
  user_no                 INT           NOT NULL,
  score                   INT           NOT NULL DEFAULT 0,
  title                   VARCHAR(30)   NOT NULL DEFAULT '아기 거지',
  total_saved_amount      BIGINT        NOT NULL DEFAULT 0,
  budget_compliance_rate  DECIMAL(5,2)  NULL,
  avg_savings_ratio       DECIMAL(5,2)  NULL,
  participation_count     INT           NOT NULL DEFAULT 0,
  last_calculated_at      DATETIME      NOT NULL,
  updated_at              DATETIME      NOT NULL,
  PRIMARY KEY (user_no),
  CONSTRAINT fk_scores_user FOREIGN KEY (user_no) REFERENCES users(user_no),
  INDEX idx_scores_score_desc (score DESC)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
