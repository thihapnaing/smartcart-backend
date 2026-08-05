-- -------------------------------------------------------------
-- H2 Test Database Schema
-- Required to initialize tables before data.sql runs in testing
-- -------------------------------------------------------------

CREATE TABLE IF NOT EXISTS category (
    id BIGINT NOT NULL,
    name VARCHAR(255),
    PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS `user` (
    id BIGINT NOT NULL,
    username VARCHAR(255),
    email VARCHAR(255),
    password VARCHAR(255),
    role VARCHAR(50),
    status VARCHAR(50),
    created_at TIMESTAMP,
    PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS user_profile (
    id BIGINT NOT NULL,
    user_id BIGINT,
    first_name VARCHAR(255),
    last_name VARCHAR(255),
    address VARCHAR(255),
    postal_code VARCHAR(50),
    phone_number VARCHAR(50),
    avatar_url VARCHAR(255),
    shop_name VARCHAR(255),
    PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS product (
    id BIGINT NOT NULL,
    name VARCHAR(255),
    description VARCHAR(500),
    price DECIMAL(10, 2),
    image_url VARCHAR(255),
    gender VARCHAR(50),
    category_id BIGINT,
    user_id BIGINT,
    shop_name VARCHAR(255),
    status VARCHAR(50),
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
