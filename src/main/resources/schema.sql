-- ============================================================
-- SMARTCART DATABASE SCHEMA
-- MySQL 8.x
--
-- Source of truth for the SmartCart application.
--
-- Main architecture:
--   Angular -> Spring Boot -> MySQL
--   Angular -> Spring Boot -> Python CNN -> Spring Boot -> MySQL
--
-- CNN image search attributes stored in PRODUCT:
--   gender  = MAN / WOMAN
--   color   = BLACK / BLUE / BROWN / GRAY / GREEN / ORANGE /
--             PINK / PURPLE / RED / WHITE / YELLOW
--   category = Shirt / Pant / Shoe
-- ============================================================

DROP DATABASE IF EXISTS smartcart_db;
CREATE DATABASE smartcart_db
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

USE smartcart_db;

SET FOREIGN_KEY_CHECKS = 0;

DROP TABLE IF EXISTS order_item;
DROP TABLE IF EXISTS orders;
DROP TABLE IF EXISTS payment;
DROP TABLE IF EXISTS cart_item;
DROP TABLE IF EXISTS cart;
DROP TABLE IF EXISTS chat_message;
DROP TABLE IF EXISTS chat_session;
DROP TABLE IF EXISTS ai_recommendations;
DROP TABLE IF EXISTS image_search_history;
DROP TABLE IF EXISTS product_moderations;
DROP TABLE IF EXISTS product;
DROP TABLE IF EXISTS category;
DROP TABLE IF EXISTS smartcart_user_profile;
DROP TABLE IF EXISTS smartcart_user;

SET FOREIGN_KEY_CHECKS = 1;


-- ============================================================
-- 1. USERS
-- ============================================================

CREATE TABLE smartcart_user (
                                id BIGINT AUTO_INCREMENT PRIMARY KEY,

                                username VARCHAR(50) NOT NULL,
                                email VARCHAR(100) NOT NULL,
                                password VARCHAR(255) NOT NULL,

                                role ENUM(
        'CUSTOMER',
        'MERCHANT',
        'ADMIN',
        'DELIVERYMAN'
    ) NOT NULL DEFAULT 'CUSTOMER',

                                status ENUM(
        'ACTIVE',
        'SUSPENDED',
        'INACTIVE'
    ) NOT NULL DEFAULT 'ACTIVE',

                                created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

                                CONSTRAINT uk_smartcart_user_username UNIQUE (username),
                                CONSTRAINT uk_smartcart_user_email UNIQUE (email),

                                INDEX idx_user_role (role),
                                INDEX idx_user_status (status)
);


-- ============================================================
-- 2. USER PROFILE
-- ============================================================

CREATE TABLE smartcart_user_profile (
                                        id BIGINT AUTO_INCREMENT PRIMARY KEY,

                                        user_id BIGINT NOT NULL UNIQUE,

                                        first_name VARCHAR(100),
                                        last_name VARCHAR(100),
                                        address VARCHAR(255),
                                        postal_code VARCHAR(20),
                                        phone_number VARCHAR(30),
                                        avatar_url VARCHAR(500),

                                        created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                        updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
                                            ON UPDATE CURRENT_TIMESTAMP,

                                        CONSTRAINT fk_user_profile_user
                                            FOREIGN KEY (user_id)
                                                REFERENCES smartcart_user(id)
                                                ON DELETE CASCADE
                                                ON UPDATE CASCADE
);


-- ============================================================
-- 3. CATEGORY
-- ============================================================

CREATE TABLE category (
                          id BIGINT AUTO_INCREMENT PRIMARY KEY,

                          name VARCHAR(100) NOT NULL,

                          CONSTRAINT uk_category_name UNIQUE (name)
);


-- ============================================================
-- 4. PRODUCT
-- ============================================================
-- IMPORTANT FOR CNN IMAGE SEARCH
--
-- gender and color are stored separately so Spring Boot can
-- query products using the CNN prediction:
--
--   gender = MAN
--   color = GREEN
--   category = Shirt
--
-- instead of searching the whole phrase "man green shirt".
-- ============================================================

CREATE TABLE product (
                         product_id BIGINT AUTO_INCREMENT PRIMARY KEY,

                         user_id BIGINT NOT NULL,
                         category_id BIGINT NOT NULL,

                         name VARCHAR(255) NOT NULL,
                         description TEXT,

                         price DECIMAL(10,2) NOT NULL,

                         gender ENUM(
        'MAN',
        'WOMAN'
    ) NULL,

                         color VARCHAR(30) NULL,

                         image_url VARCHAR(500),

                         shop_name VARCHAR(255),

                         status ENUM(
        'PENDING',
        'ACTIVE',
        'REJECTED',
        'SUSPENDED',
        'OUT_OF_STOCK',
        'INACTIVE'
    ) NOT NULL DEFAULT 'PENDING',

                         created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                         updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
                             ON UPDATE CURRENT_TIMESTAMP,

                         CONSTRAINT fk_product_user
                             FOREIGN KEY (user_id)
                                 REFERENCES smartcart_user(id)
                                 ON DELETE RESTRICT
                                 ON UPDATE CASCADE,

                         CONSTRAINT fk_product_category
                             FOREIGN KEY (category_id)
                                 REFERENCES category(id)
                                 ON DELETE RESTRICT
                                 ON UPDATE CASCADE,

                         CONSTRAINT chk_product_price
                             CHECK (price >= 0),

                         INDEX idx_product_user (user_id),
                         INDEX idx_product_category (category_id),
                         INDEX idx_product_gender (gender),
                         INDEX idx_product_color (color),
                         INDEX idx_product_status (status),

                         INDEX idx_product_image_search (gender, color, category_id)
);

CREATE TABLE IF NOT EXISTS product_variant (
                                               id BIGINT AUTO_INCREMENT PRIMARY KEY,
                                               product_id BIGINT NOT NULL,
                                               size VARCHAR(50),
    stock INT NOT NULL DEFAULT 0,
    CONSTRAINT fk_product_variant_product
    FOREIGN KEY (product_id)
    REFERENCES product(product_id)
    ON DELETE CASCADE
    );


-- ============================================================
-- 5. PRODUCT MODERATIONS
-- ============================================================

CREATE TABLE product_moderations (
                                     moderation_id BIGINT AUTO_INCREMENT PRIMARY KEY,

                                     product_id BIGINT NOT NULL,
                                     merchant_id BIGINT NOT NULL,

                                     action ENUM(
        'Approve',
        'Reject'
    ) NOT NULL,

                                     reason VARCHAR(500),

                                     created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

                                     CONSTRAINT fk_moderation_product
                                         FOREIGN KEY (product_id)
                                             REFERENCES product(product_id)
                                             ON DELETE CASCADE
                                             ON UPDATE CASCADE,

                                     CONSTRAINT fk_moderation_merchant
                                         FOREIGN KEY (merchant_id)
                                             REFERENCES smartcart_user(id)
                                             ON DELETE RESTRICT
                                             ON UPDATE CASCADE,

                                     INDEX idx_moderation_product (product_id),
                                     INDEX idx_moderation_merchant (merchant_id)
);


-- ============================================================
-- 6. IMAGE SEARCH HISTORY
-- ============================================================
-- Stores the result of the Python CNN image-search service.
-- This allows SmartCart to keep a history of customer searches.
-- ============================================================

CREATE TABLE image_search_history (
                                      history_id BIGINT AUTO_INCREMENT PRIMARY KEY,

                                      customer_id BIGINT NOT NULL,

                                      uploaded_image VARCHAR(500),

                                      predicted_gender VARCHAR(30),
                                      predicted_color VARCHAR(30),
                                      predicted_category VARCHAR(100),

                                      search_text VARCHAR(255),

                                      similarity DECIMAL(12,8),

                                      created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

                                      CONSTRAINT fk_image_history_customer
                                          FOREIGN KEY (customer_id)
                                              REFERENCES smartcart_user(id)
                                              ON DELETE CASCADE
                                              ON UPDATE CASCADE,

                                      INDEX idx_image_history_customer (customer_id),
                                      INDEX idx_image_history_created (created_at)
);


-- ============================================================
-- 7. AI RECOMMENDATIONS
-- ============================================================

CREATE TABLE ai_recommendations (
                                    id BIGINT AUTO_INCREMENT PRIMARY KEY,

                                    user_id BIGINT NOT NULL,

                                    title VARCHAR(255),
                                    content TEXT,

                                    status ENUM(
        'ACTIVE',
        'READ',
        'DISMISSED',
        'EXPIRED'
    ) NOT NULL DEFAULT 'ACTIVE',

                                    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

                                    CONSTRAINT fk_ai_recommendation_user
                                        FOREIGN KEY (user_id)
                                            REFERENCES smartcart_user(id)
                                            ON DELETE CASCADE
                                            ON UPDATE CASCADE,

                                    INDEX idx_ai_recommendation_user (user_id),
                                    INDEX idx_ai_recommendation_status (status)
);


-- ============================================================
-- 8. CHAT SESSION
-- ============================================================

CREATE TABLE chat_session (
                              id BIGINT AUTO_INCREMENT PRIMARY KEY,

                              user_id BIGINT NOT NULL,

                              title VARCHAR(255),

                              status ENUM(
        'ACTIVE',
        'CLOSED'
    ) NOT NULL DEFAULT 'ACTIVE',

                              created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                              updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
                                  ON UPDATE CURRENT_TIMESTAMP,

                              CONSTRAINT fk_chat_session_user
                                  FOREIGN KEY (user_id)
                                      REFERENCES smartcart_user(id)
                                      ON DELETE CASCADE
                                      ON UPDATE CASCADE,

                              INDEX idx_chat_session_user (user_id),
                              INDEX idx_chat_session_status (status)
);


-- ============================================================
-- 9. CHAT MESSAGE
-- ============================================================

CREATE TABLE chat_message (
                              id BIGINT AUTO_INCREMENT PRIMARY KEY,

                              session_id BIGINT NOT NULL,

                              sender_role ENUM(
        'USER',
        'ASSISTANT',
        'SYSTEM'
    ) NOT NULL,

                              content TEXT NOT NULL,

                              created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

                              CONSTRAINT fk_chat_message_session
                                  FOREIGN KEY (session_id)
                                      REFERENCES chat_session(id)
                                      ON DELETE CASCADE
                                      ON UPDATE CASCADE,

                              INDEX idx_chat_message_session (session_id),
                              INDEX idx_chat_message_created (created_at)
);


-- ============================================================
-- 10. CART
-- ============================================================

CREATE TABLE cart (
                      cart_id BIGINT AUTO_INCREMENT PRIMARY KEY,

                      customer_id BIGINT NOT NULL,

                      created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                      updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
                          ON UPDATE CURRENT_TIMESTAMP,

                      CONSTRAINT fk_cart_customer
                          FOREIGN KEY (customer_id)
                              REFERENCES smartcart_user(id)
                              ON DELETE CASCADE
                              ON UPDATE CASCADE,

                      CONSTRAINT uk_cart_customer UNIQUE (customer_id)
);


-- ============================================================
-- 11. CART ITEM
-- ============================================================

CREATE TABLE cart_item (
                           cart_item_id BIGINT AUTO_INCREMENT PRIMARY KEY,

                           cart_id BIGINT NOT NULL,
                           product_id BIGINT NOT NULL,

                           quantity INT NOT NULL DEFAULT 1,

                           created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

                           CONSTRAINT fk_cart_item_cart
                               FOREIGN KEY (cart_id)
                                   REFERENCES cart(cart_id)
                                   ON DELETE CASCADE
                                   ON UPDATE CASCADE,

                           CONSTRAINT fk_cart_item_product
                               FOREIGN KEY (product_id)
                                   REFERENCES product(product_id)
                                   ON DELETE RESTRICT
                                   ON UPDATE CASCADE,

                           CONSTRAINT chk_cart_item_quantity
                               CHECK (quantity > 0),

                           CONSTRAINT uk_cart_product
                               UNIQUE (cart_id, product_id),

                           INDEX idx_cart_item_product (product_id)
);


-- ============================================================
-- 12. PAYMENT
-- ============================================================
-- Kept as a separate table because orders.payment_id exists
-- conceptually in the SmartCart ERD.
-- ============================================================

CREATE TABLE payment (
                         payment_id BIGINT AUTO_INCREMENT PRIMARY KEY,

                         customer_id BIGINT NOT NULL,

                         amount DECIMAL(10,2) NOT NULL,

                         payment_method ENUM(
        'CARD',
        'PAYNOW',
        'CASH_ON_DELIVERY'
    ) NOT NULL,

                         status ENUM(
        'PENDING',
        'PAID',
        'FAILED',
        'REFUNDED'
    ) NOT NULL DEFAULT 'PENDING',

                         transaction_reference VARCHAR(255),

                         paid_at TIMESTAMP NULL,

                         created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

                         CONSTRAINT fk_payment_customer
                             FOREIGN KEY (customer_id)
                                 REFERENCES smartcart_user(id)
                                 ON DELETE RESTRICT
                                 ON UPDATE CASCADE,

                         CONSTRAINT uk_payment_transaction_reference
                             UNIQUE (transaction_reference),

                         CONSTRAINT chk_payment_amount
                             CHECK (amount >= 0),

                         INDEX idx_payment_customer (customer_id),
                         INDEX idx_payment_status (status)
);


-- ============================================================
-- 13. ORDERS
-- ============================================================
-- The table is named "orders", not "order".
-- Java entity can still be named Order.
-- ============================================================

CREATE TABLE orders (
                        order_id BIGINT AUTO_INCREMENT PRIMARY KEY,

                        customer_id BIGINT NOT NULL,

                        payment_id BIGINT NULL,

                        order_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

                        total_amount DECIMAL(10,2) NOT NULL DEFAULT 0.00,

                        status ENUM(
        'Pending',
        'Paid',
        'Packed',
        'Delivered',
        'Cancelled'
    ) NOT NULL DEFAULT 'Pending',

                        shipping_address VARCHAR(500),

                        delivered_at TIMESTAMP NULL,

                        CONSTRAINT fk_order_customer
                            FOREIGN KEY (customer_id)
                                REFERENCES smartcart_user(id)
                                ON DELETE RESTRICT
                                ON UPDATE CASCADE,

                        CONSTRAINT fk_order_payment
                            FOREIGN KEY (payment_id)
                                REFERENCES payment(payment_id)
                                ON DELETE SET NULL
                                ON UPDATE CASCADE,

                        CONSTRAINT chk_order_total_amount
                            CHECK (total_amount >= 0),

                        INDEX idx_order_customer (customer_id),
                        INDEX idx_order_payment (payment_id),
                        INDEX idx_order_status (status),
                        INDEX idx_order_date (order_date)
);


-- ============================================================
-- 14. ORDER ITEM
-- ============================================================

CREATE TABLE order_item (
                            order_item_id BIGINT AUTO_INCREMENT PRIMARY KEY,

                            order_id BIGINT NOT NULL,

                            product_id BIGINT NOT NULL,

                            quantity INT NOT NULL DEFAULT 1,

                            unit_price DECIMAL(10,2) NOT NULL,

                            CONSTRAINT fk_order_item_order
                                FOREIGN KEY (order_id)
                                    REFERENCES orders(order_id)
                                    ON DELETE CASCADE
                                    ON UPDATE CASCADE,

                            CONSTRAINT fk_order_item_product
                                FOREIGN KEY (product_id)
                                    REFERENCES product(product_id)
                                    ON DELETE RESTRICT
                                    ON UPDATE CASCADE,

                            CONSTRAINT chk_order_item_quantity
                                CHECK (quantity > 0),

                            CONSTRAINT chk_order_item_unit_price
                                CHECK (unit_price >= 0),

                            INDEX idx_order_item_order (order_id),
                            INDEX idx_order_item_product (product_id)
);


-- ============================================================
-- 15. INITIAL CATEGORY DATA
-- ============================================================
-- These IDs match the current SmartCart product seed:
--   1 = Shirt
--   2 = Pant
--   3 = Shoe
-- ============================================================

INSERT INTO category (id, name)
VALUES
    (1, 'Shirt'),
    (2, 'Pant'),
    (3, 'Shoe');


-- ============================================================
-- 16. CNN COLOR REFERENCE
-- ============================================================
-- Product.color is intentionally VARCHAR rather than ENUM so
-- the database can support future CNN labels without requiring
-- a schema change.
--
-- Current CNN colors:
-- BLACK, BLUE, BROWN, GRAY, GREEN, ORANGE, PINK, PURPLE,
-- RED, WHITE, YELLOW
--
-- Existing product seed may also contain values such as:
-- NAVY, CREAM, CORAL, BEIGE.
-- These are allowed in product.color.
-- ============================================================

-- Example:
-- UPDATE product SET color = 'GREEN' WHERE name = 'Green Shirt';


-- ============================================================
-- END OF SMARTCART SCHEMA
-- ============================================================