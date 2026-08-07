DROP TABLE IF EXISTS product;
DROP TABLE IF EXISTS category;

CREATE TABLE category (
                          id BIGINT AUTO_INCREMENT PRIMARY KEY,
                          name VARCHAR(50) NOT NULL UNIQUE,
                          description VARCHAR(255),
                          created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE product (
                         id BIGINT PRIMARY KEY,

                         name VARCHAR(255) NOT NULL,

                         description TEXT,

                         gender ENUM('MAN','WOMAN') NOT NULL,

                         color VARCHAR(50) NOT NULL,

                         price DECIMAL(10,2) NOT NULL,

                         image_url VARCHAR(255) NOT NULL,

                         shop_name VARCHAR(100) NOT NULL,

                         status ENUM('ACTIVE','INACTIVE')
        NOT NULL DEFAULT 'ACTIVE',

                         created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

                         updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                             ON UPDATE CURRENT_TIMESTAMP,

                         category_id BIGINT NOT NULL,

                         CONSTRAINT fk_product_category
                             FOREIGN KEY (category_id)
                                 REFERENCES category(id)
);