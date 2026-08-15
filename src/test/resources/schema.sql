-- -------------------------------------------------------------
-- H2 Test Database Schema
-- Required to initialize tables before data.sql runs in testing
-- -------------------------------------------------------------

CREATE TABLE IF NOT EXISTS category (
    id BIGINT NOT NULL,
    name VARCHAR(255),
    PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS `smartcart_user` (
    id BIGINT NOT NULL,
    username VARCHAR(255),
    email VARCHAR(255),
    password VARCHAR(255),
    role VARCHAR(50),
    status VARCHAR(50),
    created_at TIMESTAMP,
    PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS smartcart_user_profile (
    id BIGINT NOT NULL,
    user_id BIGINT,
    first_name VARCHAR(255),
    last_name VARCHAR(255),
    address VARCHAR(255),
    postal_code VARCHAR(50),
    phone_number VARCHAR(50),
    avatar_url VARCHAR(255),
    shop_name VARCHAR(255),
    interests VARCHAR(255),
    preferred_categories VARCHAR(255),
    budget DECIMAL(10, 2),
    PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS smartcart_merchant (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL UNIQUE,
    business_name VARCHAR(150) NOT NULL,
    uen VARCHAR(50) NOT NULL UNIQUE,
    business_type VARCHAR(100),
    business_address VARCHAR(255),
    postal_code VARCHAR(20),
    contact_number VARCHAR(30),
    product_category VARCHAR(100),
    business_description TEXT,
    logo_url VARCHAR(500),
    registration_document_url VARCHAR(500),
    pickup_available BOOLEAN NOT NULL DEFAULT FALSE,
    verification_status VARCHAR(30) NOT NULL DEFAULT 'PENDING',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
        ON UPDATE CURRENT_TIMESTAMP,

    CONSTRAINT fk_merchant_profile_user
        FOREIGN KEY (user_id)
            REFERENCES smartcart_user(id)
            ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS product (
    id BIGINT NOT NULL,
    name VARCHAR(255),
    description VARCHAR(500),
    price DECIMAL(10, 2),
    image_url VARCHAR(255),
    gender VARCHAR(50),
    color VARCHAR(255) NOT NULL,
    category_id BIGINT,
    user_id BIGINT,
    shop_name VARCHAR(255),
    status VARCHAR(50),
    admin_locked BOOLEAN,
    created_at TIMESTAMP,
    PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS product_variant (
    id BIGINT NOT NULL,
    product_id BIGINT,
    size VARCHAR(50),
    stock INT,
    PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS orders (
    id BIGINT NOT NULL,
    user_id BIGINT,
    total_amount DECIMAL(10, 2),
    status VARCHAR(50),
    first_name VARCHAR(255),
    last_name VARCHAR(255),
    shipping_address VARCHAR(500),
    phone_number VARCHAR(50),
    delivered_at TIMESTAMP,
    order_date TIMESTAMP,
    delivery_person_id BIGINT,
    delivery_proof_key VARCHAR(255),
    tracking_number VARCHAR(50),
    PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS order_item (
    id BIGINT NOT NULL,
    order_id BIGINT,
    product_variant_id BIGINT,
    quantity INT,
    unit_price DECIMAL(10, 2),
    PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS payment (
    id BIGINT NOT NULL,
    order_id BIGINT,
    payment_method VARCHAR(50),
    paid_at TIMESTAMP,
    PRIMARY KEY (id)
);
