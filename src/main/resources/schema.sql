-- SmartCart product/image-search schema
-- Generated from the current JPA entities and labels(4).csv.
-- MySQL 8.x
--
-- Covers the entities required by the current image-search flow:
-- smartcart_user, category, product, product_variant.
-- It does not create unrelated SmartCart tables.

CREATE TABLE IF NOT EXISTS smartcart_user (
                                              id BIGINT NOT NULL AUTO_INCREMENT,
                                              username VARCHAR(100),
    email VARCHAR(255),
    password VARCHAR(255),
    role VARCHAR(30),
    status VARCHAR(30),
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_smartcart_user_username (username),
    UNIQUE KEY uk_smartcart_user_email (email),
    KEY idx_smartcart_user_role (role),
    KEY idx_smartcart_user_status (status)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS category (
                                        id BIGINT NOT NULL AUTO_INCREMENT,
                                        name VARCHAR(100) NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_category_name (name)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS product (
                                       id BIGINT NOT NULL AUTO_INCREMENT,
                                       name VARCHAR(255) NOT NULL,
    description TEXT,
    price DECIMAL(10,2) NOT NULL,
    image_url VARCHAR(1000),
    gender VARCHAR(20) NOT NULL,
    color VARCHAR(100) NOT NULL,
    category_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    shop_name VARCHAR(255),
    status VARCHAR(20),
    admin_locked BOOLEAN DEFAULT FALSE,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_product_image_search (gender, color, category_id, status),
    KEY idx_product_category (category_id),
    KEY idx_product_user (user_id),
    KEY idx_product_status (status),
    CONSTRAINT fk_product_category
    FOREIGN KEY (category_id) REFERENCES category(id),
    CONSTRAINT fk_product_user
    FOREIGN KEY (user_id) REFERENCES smartcart_user(id)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS product_variant (
                                               id BIGINT NOT NULL AUTO_INCREMENT,
                                               product_id BIGINT NOT NULL,
                                               size VARCHAR(50),
    stock INT,
    PRIMARY KEY (id),
    UNIQUE KEY uk_product_variant_product_size (product_id, size),
    KEY idx_product_variant_product (product_id),
    CONSTRAINT fk_product_variant_product
    FOREIGN KEY (product_id) REFERENCES product(id)
    ON DELETE CASCADE
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
