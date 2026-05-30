-- 거지 우정 수호대 — 로컬 MySQL 초기화 스크립트
-- 사용법: mysql -u root -p < sql/init.sql
-- 또는 MySQL Workbench에서 통째로 실행

CREATE DATABASE IF NOT EXISTS beggar
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

USE beggar;

-- ─── 10개 테이블 DDL (docs/DB_DESIGN.md 와 동일) ─────────────────────────────

-- 1. users
CREATE TABLE IF NOT EXISTS users (
  user_no            INT          NOT NULL AUTO_INCREMENT,
  user_name          VARCHAR(15)  NOT NULL,
  password_hash      VARCHAR(255) NULL,
  profile_image_url  VARCHAR(500) NULL,
  uemail             VARCHAR(100) NOT NULL,
  gender             VARCHAR(20)  NULL,
  age_range          VARCHAR(20)  NULL,
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
  max_member_count INT        NOT NULL DEFAULT 100,
  total_budget  INT          NULL,
  room_created  DATETIME     NOT NULL,
  is_friends    BOOLEAN      NOT NULL,    -- 현재 MVP는 친구 초대 기반 방 중심. FALSE는 공개 방 후보로만 보존
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
  receipt_type    VARCHAR(20)    NOT NULL,
  input_method    VARCHAR(20)    NOT NULL,
  image_url       VARCHAR(500)   NULL,
  ocr_status      VARCHAR(30)    NOT NULL,
  store_name      VARCHAR(150)   NULL,
  total_amount    INT            NULL,
  amount          INT            NOT NULL,
  address         VARCHAR(100)   NULL,
  center_lat      DECIMAL(10,7)  NULL,
  center_lng      DECIMAL(10,7)  NULL,
  good_price_store_id       VARCHAR(100)  NULL,
  good_price_store_name     VARCHAR(150)  NULL,
  good_price_store_address  VARCHAR(200)  NULL,
  good_price_matched        BOOLEAN       NOT NULL DEFAULT FALSE,
  good_price_verified_at    DATETIME      NULL,
  created_at      DATETIME       NOT NULL,
  updated_at      DATETIME       NOT NULL,
  PRIMARY KEY (receipt_id),
  CONSTRAINT fk_receipts_room   FOREIGN KEY (room_no)        REFERENCES rooms(room_no),
  CONSTRAINT fk_receipts_member FOREIGN KEY (room_member_id) REFERENCES room_members(room_member_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 8. receipt_splits
CREATE TABLE IF NOT EXISTS receipt_splits (
  split_id        INT       NOT NULL AUTO_INCREMENT,
  receipt_id      INT       NOT NULL,
  room_member_id  INT       NOT NULL,
  amount          INT       NOT NULL,
  created_at      DATETIME  NOT NULL,
  updated_at      DATETIME  NOT NULL,
  PRIMARY KEY (split_id),
  UNIQUE KEY uk_receipt_splits_receipt_member (receipt_id, room_member_id),
  CONSTRAINT fk_receipt_splits_receipt FOREIGN KEY (receipt_id) REFERENCES receipts(receipt_id),
  CONSTRAINT fk_receipt_splits_member  FOREIGN KEY (room_member_id) REFERENCES room_members(room_member_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 9. room_beggar_scores
CREATE TABLE IF NOT EXISTS room_beggar_scores (
  score_id                    INT           NOT NULL AUTO_INCREMENT,
  room_no                     INT           NOT NULL,
  score                       INT           NOT NULL DEFAULT 0,
  title                       VARCHAR(30)   NOT NULL DEFAULT '아기 거지',
  total_spent_amount          BIGINT        NOT NULL DEFAULT 0,
  total_saved_amount          BIGINT        NOT NULL DEFAULT 0,
  good_price_verified_count   INT           NOT NULL DEFAULT 0,
  budget_compliance_rate      DECIMAL(5,2)  NULL,
  avg_savings_ratio           DECIMAL(5,2)  NULL,
  last_calculated_at          DATETIME      NOT NULL,
  updated_at                  DATETIME      NOT NULL,
  PRIMARY KEY (score_id),
  UNIQUE KEY uk_room_beggar_scores_room (room_no),
  CONSTRAINT fk_room_beggar_scores_room FOREIGN KEY (room_no) REFERENCES rooms(room_no),
  INDEX idx_room_scores_score_desc (score DESC)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 10. community_posts
CREATE TABLE IF NOT EXISTS community_posts (
  post_id     INT           NOT NULL AUTO_INCREMENT,
  user_no     INT           NOT NULL,
  title       VARCHAR(100)  NOT NULL,
  content     TEXT          NOT NULL,
  category    VARCHAR(30)   NULL,
  created_at  DATETIME      NOT NULL,
  updated_at  DATETIME      NOT NULL,
  PRIMARY KEY (post_id),
  CONSTRAINT fk_community_posts_user FOREIGN KEY (user_no) REFERENCES users(user_no),
  INDEX idx_community_posts_created_at (created_at DESC)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
