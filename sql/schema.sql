-- Banking Management System — normalized schema
-- Charset: utf8mb4 for full Unicode support

CREATE DATABASE IF NOT EXISTS bank_mgmt
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;

USE bank_mgmt;

-- Registered users (admins are users with role ADMIN + row in admins)
CREATE TABLE IF NOT EXISTS users (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(64) NOT NULL UNIQUE,
    email VARCHAR(128) NOT NULL UNIQUE,
    password_hash VARCHAR(256) NOT NULL,
    salt VARCHAR(64) NOT NULL,
    role ENUM('ADMIN','USER') NOT NULL DEFAULT 'USER',
    full_name VARCHAR(128) NOT NULL,
    phone VARCHAR(32) NULL,
    security_question VARCHAR(255) NOT NULL,
    security_answer_hash VARCHAR(256) NOT NULL,
    salt_answer VARCHAR(64) NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_users_username (username),
    INDEX idx_users_email (email),
    INDEX idx_users_role (role)
) ENGINE=InnoDB;

-- Explicit admin linkage (1:1 with users.id where role = ADMIN)
CREATE TABLE IF NOT EXISTS admins (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT UNSIGNED NOT NULL UNIQUE,
    CONSTRAINT fk_admins_user FOREIGN KEY (user_id) REFERENCES users (id)
        ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS accounts (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    account_number VARCHAR(24) NOT NULL UNIQUE,
    user_id BIGINT UNSIGNED NOT NULL,
    balance DECIMAL(15,2) NOT NULL DEFAULT 0.00,
    status ENUM('ACTIVE','FROZEN','CLOSED') NOT NULL DEFAULT 'ACTIVE',
    account_type VARCHAR(32) NOT NULL DEFAULT 'SAVINGS',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_accounts_user FOREIGN KEY (user_id) REFERENCES users (id)
        ON DELETE CASCADE ON UPDATE CASCADE,
    INDEX idx_accounts_user (user_id),
    INDEX idx_accounts_status (status)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS transactions (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    from_account_id BIGINT UNSIGNED NULL,
    to_account_id BIGINT UNSIGNED NULL,
    amount DECIMAL(15,2) NOT NULL,
    transaction_type ENUM('DEPOSIT','WITHDRAW','TRANSFER') NOT NULL,
    description VARCHAR(512) NULL,
    balance_after_from DECIMAL(15,2) NULL,
    balance_after_to DECIMAL(15,2) NULL,
    performed_by_user_id BIGINT UNSIGNED NOT NULL,
    receipt_ref VARCHAR(36) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_tx_from FOREIGN KEY (from_account_id) REFERENCES accounts (id)
        ON DELETE RESTRICT ON UPDATE CASCADE,
    CONSTRAINT fk_tx_to FOREIGN KEY (to_account_id) REFERENCES accounts (id)
        ON DELETE RESTRICT ON UPDATE CASCADE,
    CONSTRAINT fk_tx_user FOREIGN KEY (performed_by_user_id) REFERENCES users (id)
        ON DELETE RESTRICT ON UPDATE CASCADE,
    INDEX idx_transactions_created (created_at),
    INDEX idx_transactions_from (from_account_id),
    INDEX idx_transactions_to (to_account_id),
    INDEX idx_transactions_receipt (receipt_ref)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS activity_logs (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT UNSIGNED NULL,
    action VARCHAR(128) NOT NULL,
    details VARCHAR(1024) NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_activity_user FOREIGN KEY (user_id) REFERENCES users (id)
        ON DELETE SET NULL ON UPDATE CASCADE,
    INDEX idx_activity_created (created_at),
    INDEX idx_activity_action (action)
) ENGINE=InnoDB;

-- Seed default admin (password: Admin@123) — salt / hashes produced by PasswordHasher in app
-- Replace after first login or regenerate using RegisterAdmin utility if preferred.
INSERT IGNORE INTO users (
    username, email, password_hash, salt, role, full_name, phone,
    security_question, security_answer_hash, salt_answer, is_active
) VALUES (
    'admin',
    'admin@bankmgmt.local',
    '8c8746be97b5fda756b26fb956ac8f714ffe6b9781563d9c65320acc221a8e7a',
    'a1b2c3d4e5f60718293a4b5c6d7e8f90',
    'ADMIN',
    'System Administrator',
    NULL,
    'What is the default backup passphrase?',
    '7b539ed487596047d032bfc627644ab56a230937993961c953ed7791767d6f31',
    'f9e8d7c6b5a493827160f9e8d7c6b5a4',
    TRUE
);

INSERT IGNORE INTO admins (user_id)
SELECT id FROM users WHERE username = 'admin' LIMIT 1;
