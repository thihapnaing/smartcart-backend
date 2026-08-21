-- Author: Htet Nandar (Grace)
-- ===================================================================
-- Seed / demo data for SmartCart.
-- Runs on every startup (spring.sql.init.mode=always), AFTER Hibernate's
-- ddl-auto=update has created/updated the schema (spring.jpa.defer-
-- datasource-initialization=true). Uses INSERT ... ON DUPLICATE KEY UPDATE
-- throughout (not INSERT IGNORE) so re-running this file - e.g. after
-- editing a row here - actually refreshes existing rows instead of
-- silently skipping them because the id already exists.
--
-- created_at / order_date / paid_at are set explicitly for every row:
-- data.sql runs as raw SQL and never goes through the Hibernate entity
-- lifecycle, so @PrePersist callbacks never fire for these rows.
-- ===================================================================

-- -------------------------------------------------------------
-- Categories
-- -------------------------------------------------------------
INSERT INTO category (id, name) VALUES
                                    (1, 'Tops'),
                                    (2, 'Bottoms'),
                                    (3, 'Shoes')
    ON DUPLICATE KEY UPDATE name = VALUES(name);

-- -------------------------------------------------------------
-- Users. Password for every seeded account is "password123", stored as a
-- real BCrypt hash (strength 10, matching SecurityConfig's
-- BCryptPasswordEncoder()) so POST /api/auth/login actually works against
-- these rows - the previous plaintext placeholder always failed
-- passwordEncoder.matches() once real login was wired up.
-- -------------------------------------------------------------
INSERT INTO `smartcart_user` (id, username, email, password, role, status, created_at) VALUES
                                                                                           (1, 'smartcart_official', 'merchant@smartcart.demo', '$2b$10$ZY6WZo/5w8s3aeZPuz2wFOAt6AcLDrxOC.zfhgRTf4udd.KkjHJj6', 'MERCHANT', 'ACTIVE', DATE_SUB(NOW(), INTERVAL 200 DAY)),
                                                                                           (2, 'grace', 'grace@smartcart.demo', '$2b$10$ZY6WZo/5w8s3aeZPuz2wFOAt6AcLDrxOC.zfhgRTf4udd.KkjHJj6', 'CUSTOMER', 'ACTIVE', DATE_SUB(NOW(), INTERVAL 120 DAY)),
                                                                                           (3, 'alex', 'alex@smartcart.demo', '$2b$10$ZY6WZo/5w8s3aeZPuz2wFOAt6AcLDrxOC.zfhgRTf4udd.KkjHJj6', 'CUSTOMER', 'ACTIVE', DATE_SUB(NOW(), INTERVAL 90 DAY)),
                                                                                           (4, 'admin', 'admin@smartcart.demo', '$2b$10$ZY6WZo/5w8s3aeZPuz2wFOAt6AcLDrxOC.zfhgRTf4udd.KkjHJj6', 'ADMIN', 'ACTIVE', DATE_SUB(NOW(), INTERVAL 200 DAY))
    ON DUPLICATE KEY UPDATE
                         username = VALUES(username), email = VALUES(email), password = VALUES(password),
                         role = VALUES(role), status = VALUES(status), created_at = VALUES(created_at);

INSERT INTO smartcart_user_profile (id, user_id, first_name, last_name, address, postal_code, phone_number, avatar_url, shop_name, interests, preferred_categories, budget) VALUES
                                                                                                                                                                                (1, 1, 'SmartCart', 'Official', '1 Store Road, Singapore', '018956', '65001234', NULL, 'SmartCart Official', NULL, NULL, NULL),
                                                                                                                                                                                (2, 2, 'Grace', 'Tan', '123 Orchard Road, Singapore', '238888', '91234567', NULL, NULL, 'Linen,Breathable,Summer', 'Tops,Bottoms', 200.00),
                                                                                                                                                                                (3, 3, 'Alex', 'Lim', '45 Bukit Timah Road, Singapore', '229899', '98765432', NULL, NULL, 'Casual,Cotton,Minimalist', 'Tops,Shoes', 150.00),
                                                                                                                                                                                (4, 4, 'SmartCart', 'Admin', NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL)
    ON DUPLICATE KEY UPDATE
                         user_id = VALUES(user_id), first_name = VALUES(first_name), last_name = VALUES(last_name),
                         address = VALUES(address), postal_code = VALUES(postal_code), phone_number = VALUES(phone_number),
                         avatar_url = VALUES(avatar_url), shop_name = VALUES(shop_name), interests = VALUES(interests),
                         preferred_categories = VALUES(preferred_categories), budget = VALUES(budget);

-- -------------------------------------------------------------
-- Products. image_url points to real photo files bundled locally in the
-- Angular app (frontend's public/assets/products/*.jpg, served at
-- /assets/products/*.jpg) - no external hosts, no broken-link risk. Every
-- image below is genuine photography (no auto-generated placeholder
-- cards), and Tops/Bottoms/Shoes only - no dresses. 13 Tops, 13 Bottoms,
-- 14 Shoes.
-- -------------------------------------------------------------
INSERT INTO product (id, name, description, price, image_url, gender, color, category_id, user_id, shop_name, status, created_at) VALUES
                                                                                                                               -- Tops
                                                                                                                               (1, 'Classic Crew Tee', 'A soft, everyday cotton crew-neck tee.', 19.90, '/assets/products/tee-crew.jpg', 'MEN', 'White', 1, 1, 'SmartCart Official', 'ACTIVE', DATE_SUB(NOW(), INTERVAL 1 DAY)),
                                                                                                                               (2, 'Linen Shirt', 'Breathable linen button-up with a relaxed collar.', 39.90, '/assets/products/linen-shirt.jpg', 'MEN', 'Beige', 1, 1, 'SmartCart Official', 'ACTIVE', DATE_SUB(NOW(), INTERVAL 3 DAY)),
                                                                                                                               (3, 'Oversized Pocket Tee', 'Heavyweight oversized tee with a chest pocket.', 24.90, '/assets/products/tee-pocket.jpg', 'MEN', 'Grey', 1, 1, 'SmartCart Official', 'ACTIVE', DATE_SUB(NOW(), INTERVAL 2 DAY)),
                                                                                                                               (11, 'Denim Jacket', 'Classic mid-wash denim jacket, unlined.', 69.90, '/assets/products/denim-jacket.jpg', 'MEN', 'Blue', 1, 1, 'SmartCart Official', 'ACTIVE', DATE_SUB(NOW(), INTERVAL 1 DAY)),
                                                                                                                               (12, 'Hoodie', 'Heavyweight fleece hoodie with kangaroo pocket.', 44.90, '/assets/products/hoodie.jpg', 'MEN', 'Black', 1, 1, 'SmartCart Official', 'ACTIVE', DATE_SUB(NOW(), INTERVAL 2 DAY)),
                                                                                                                               (13, 'Polo Shirt', 'Pique cotton polo with a classic collar.', 34.90, '/assets/products/polo-shirt.jpg', 'MEN', 'Navy', 1, 1, 'SmartCart Official', 'ACTIVE', DATE_SUB(NOW(), INTERVAL 3 DAY)),
                                                                                                                               (14, 'Flannel Shirt', 'Brushed cotton flannel in a check print.', 39.90, '/assets/products/flannel-shirt.jpg', 'MEN', 'Red', 1, 1, 'SmartCart Official', 'ACTIVE', DATE_SUB(NOW(), INTERVAL 4 DAY)),
                                                                                                                               (15, 'Turtleneck Sweater', 'Ribbed turtleneck sweater for layering.', 44.90, '/assets/products/turtleneck-sweater.jpg', 'WOMEN', 'Cream', 1, 1, 'SmartCart Official', 'ACTIVE', DATE_SUB(NOW(), INTERVAL 5 DAY)),
                                                                                                                               (16, 'Henley Shirt', 'Waffle-knit henley with a button placket.', 29.90, '/assets/products/henley-shirt.jpg', 'MEN', 'Olive', 1, 1, 'SmartCart Official', 'ACTIVE', DATE_SUB(NOW(), INTERVAL 6 DAY)),
                                                                                                                               (17, 'Quilted Vest', 'Lightweight quilted vest, packable warmth.', 54.90, '/assets/products/quilted-vest.jpg', 'WOMEN', 'Black', 1, 1, 'SmartCart Official', 'ACTIVE', DATE_SUB(NOW(), INTERVAL 7 DAY)),
                                                                                                                               (18, 'Knit Cardigan', 'Cable-knit cardigan with button front.', 49.90, '/assets/products/knit-cardigan.jpg', 'WOMEN', 'Brown', 1, 1, 'SmartCart Official', 'ACTIVE', DATE_SUB(NOW(), INTERVAL 8 DAY)),
                                                                                                                               (19, 'Bomber Jacket', 'Lightweight bomber jacket, ribbed cuffs.', 74.90, '/assets/products/bomber-jacket.jpg', 'MEN', 'Black', 1, 1, 'SmartCart Official', 'ACTIVE', DATE_SUB(NOW(), INTERVAL 9 DAY)),
                                                                                                                               (20, 'Striped Shirt', 'Short-sleeve shirt in a bold stripe print.', 34.90, '/assets/products/striped-shirt.jpg', 'MEN', 'Blue', 1, 1, 'SmartCart Official', 'ACTIVE', DATE_SUB(NOW(), INTERVAL 10 DAY)),
                                                                                                                               -- Bottoms
                                                                                                                               (4, 'Chino Shorts', 'Relaxed-fit cotton chino shorts.', 34.90, '/assets/products/chino-shorts.jpg', 'MEN', 'Khaki', 2, 1, 'SmartCart Official', 'ACTIVE', DATE_SUB(NOW(), INTERVAL 10 DAY)),
                                                                                                                               (5, 'Straight Leg Jeans', 'Classic straight leg denim jeans.', 49.90, '/assets/products/jeans-folded.jpg', 'MEN', 'Blue', 2, 1, 'SmartCart Official', 'ACTIVE', DATE_SUB(NOW(), INTERVAL 4 DAY)),
                                                                                                                               (6, 'Wide-Leg Trousers', 'Flowy wide-leg trousers in olive cotton twill.', 44.90, '/assets/products/wide-leg-trousers.jpg', 'WOMEN', 'Olive', 2, 1, 'SmartCart Official', 'ACTIVE', DATE_SUB(NOW(), INTERVAL 6 DAY)),
                                                                                                                               (21, 'Cargo Pants', 'Utility cargo pants with side pockets.', 49.90, '/assets/products/cargo-pants.jpg', 'WOMEN', 'Beige', 2, 1, 'SmartCart Official', 'ACTIVE', DATE_SUB(NOW(), INTERVAL 1 DAY)),
                                                                                                                               (22, 'Denim Shorts', 'Printed denim shorts with a mid-rise fit.', 29.90, '/assets/products/denim-shorts.jpg', 'WOMEN', 'Blue', 2, 1, 'SmartCart Official', 'ACTIVE', DATE_SUB(NOW(), INTERVAL 2 DAY)),
                                                                                                                               (23, 'Track Pants', 'Striped track pants with a tapered leg.', 44.90, '/assets/products/track-pants.jpg', 'WOMEN', 'Grey', 2, 1, 'SmartCart Official', 'ACTIVE', DATE_SUB(NOW(), INTERVAL 3 DAY)),
                                                                                                                               (24, 'Pleated Skirt', 'A-line pleated mini skirt, elastic waist.', 34.90, '/assets/products/pleated-skirt.jpg', 'WOMEN', 'Black', 2, 1, 'SmartCart Official', 'ACTIVE', DATE_SUB(NOW(), INTERVAL 4 DAY)),
                                                                                                                               (25, 'Corduroy Pants', 'Wide-wale corduroy trousers.', 49.90, '/assets/products/corduroy-pants.jpg', 'MEN', 'Brown', 2, 1, 'SmartCart Official', 'ACTIVE', DATE_SUB(NOW(), INTERVAL 5 DAY)),
                                                                                                                               (26, 'Linen Trousers', 'Breathable linen trousers with side pockets.', 44.90, '/assets/products/linen-trousers.jpg', 'MEN', 'Beige', 2, 1, 'SmartCart Official', 'ACTIVE', DATE_SUB(NOW(), INTERVAL 6 DAY)),
                                                                                                                               (27, 'Bike Shorts', 'Stretch bike shorts for training or layering.', 24.90, '/assets/products/bike-shorts.jpg', 'WOMEN', 'Black', 2, 1, 'SmartCart Official', 'ACTIVE', DATE_SUB(NOW(), INTERVAL 7 DAY)),
                                                                                                                               (28, 'Denim Overalls', 'Classic denim overalls with adjustable straps.', 59.90, '/assets/products/denim-overalls.jpg', 'MEN', 'Blue', 2, 1, 'SmartCart Official', 'ACTIVE', DATE_SUB(NOW(), INTERVAL 8 DAY)),
                                                                                                                               (29, 'Culottes', 'Wide-leg cropped culottes in a print.', 39.90, '/assets/products/culottes.jpg', 'WOMEN', 'Beige', 2, 1, 'SmartCart Official', 'ACTIVE', DATE_SUB(NOW(), INTERVAL 9 DAY)),
                                                                                                                               (30, 'Sweatpants', 'Fleece-lined jogger sweatpants.', 34.90, '/assets/products/sweatpants.jpg', 'WOMEN', 'Grey', 2, 1, 'SmartCart Official', 'ACTIVE', DATE_SUB(NOW(), INTERVAL 10 DAY)),
                                                                                                                               -- Shoes
                                                                                                                               (7, 'Everyday Sneakers', 'Lightweight everyday sneakers that go with everything.', 69.90, '/assets/products/sneakers.jpg', 'MEN', 'White', 3, 1, 'SmartCart Official', 'ACTIVE', DATE_SUB(NOW(), INTERVAL 7 DAY)),
                                                                                                                               (8, 'Leather Ankle Boots', 'Heeled ankle boots in smooth leather.', 89.90, '/assets/products/ankle-boots.jpg', 'WOMEN', 'Black', 3, 1, 'SmartCart Official', 'ACTIVE', DATE_SUB(NOW(), INTERVAL 15 DAY)),
                                                                                                                               (9, 'Strappy Sandals', 'Single-strap leather sandals with a low heel.', 44.90, '/assets/products/strappy-sandals.jpg', 'WOMEN', 'Tan', 3, 1, 'SmartCart Official', 'ACTIVE', DATE_SUB(NOW(), INTERVAL 12 DAY)),
                                                                                                                               (10, 'Running Shoes', 'Lightweight breathable trainers with cushioned sole.', 79.90, '/assets/products/running-shoes.jpg', 'MEN', 'Grey', 3, 1, 'SmartCart Official', 'ACTIVE', DATE_SUB(NOW(), INTERVAL 9 DAY)),
                                                                                                                               (31, 'Canvas Sneakers', 'Minimalist canvas-style leather sneakers.', 59.90, '/assets/products/canvas-sneakers.jpg', 'MEN', 'White', 3, 1, 'SmartCart Official', 'ACTIVE', DATE_SUB(NOW(), INTERVAL 1 DAY)),
                                                                                                                               (32, 'High-Top Sneakers', 'Classic high-top sneakers with rubber sole.', 74.90, '/assets/products/high-top-sneakers.jpg', 'MEN', 'Black', 3, 1, 'SmartCart Official', 'ACTIVE', DATE_SUB(NOW(), INTERVAL 2 DAY)),
                                                                                                                               (33, 'Leather Loafers', 'Penny loafers in polished leather.', 79.90, '/assets/products/leather-loafers.jpg', 'MEN', 'Brown', 3, 1, 'SmartCart Official', 'ACTIVE', DATE_SUB(NOW(), INTERVAL 3 DAY)),
                                                                                                                               (34, 'Flip Flops', 'Everyday rubber flip flops.', 19.90, '/assets/products/flip-flops.jpg', 'MEN', 'Black', 3, 1, 'SmartCart Official', 'ACTIVE', DATE_SUB(NOW(), INTERVAL 4 DAY)),
                                                                                                                               (35, 'Combat Boots', 'Lace-up combat boots with a chunky sole.', 94.90, '/assets/products/combat-boots.jpg', 'WOMEN', 'Black', 3, 1, 'SmartCart Official', 'ACTIVE', DATE_SUB(NOW(), INTERVAL 5 DAY)),
                                                                                                                               (36, 'Espadrilles', 'Canvas espadrilles with a woven jute sole.', 39.90, '/assets/products/espadrilles.jpg', 'WOMEN', 'Beige', 3, 1, 'SmartCart Official', 'ACTIVE', DATE_SUB(NOW(), INTERVAL 6 DAY)),
                                                                                                                               (37, 'Slide Sandals', 'Cushioned slide sandals for everyday wear.', 24.90, '/assets/products/slide-sandals.jpg', 'MEN', 'Black', 3, 1, 'SmartCart Official', 'ACTIVE', DATE_SUB(NOW(), INTERVAL 7 DAY)),
                                                                                                                               (38, 'Hiking Boots', 'Waterproof leather hiking boots.', 99.90, '/assets/products/hiking-boots.jpg', 'MEN', 'Brown', 3, 1, 'SmartCart Official', 'ACTIVE', DATE_SUB(NOW(), INTERVAL 8 DAY)),
                                                                                                                               (39, 'Ballet Flats', 'Soft leather ballet flats.', 44.90, '/assets/products/ballet-flats.jpg', 'WOMEN', 'Nude', 3, 1, 'SmartCart Official', 'ACTIVE', DATE_SUB(NOW(), INTERVAL 9 DAY)),
                                                                                                                               (40, 'Platform Sneakers', 'Chunky platform sneakers with a colorful sole.', 64.90, '/assets/products/platform-sneakers.jpg', 'WOMEN', 'White', 3, 1, 'SmartCart Official', 'ACTIVE', DATE_SUB(NOW(), INTERVAL 10 DAY))
    ON DUPLICATE KEY UPDATE
                         name = VALUES(name), description = VALUES(description), price = VALUES(price),
                         image_url = VALUES(image_url), gender = VALUES(gender), color = VALUES(color),
                         category_id = VALUES(category_id), user_id = VALUES(user_id), shop_name = VALUES(shop_name),
                         status = VALUES(status), created_at = VALUES(created_at);

-- -------------------------------------------------------------
-- Product variants (tops/bottoms: S/M/L, shoes: sizes 39/40/41)
-- -------------------------------------------------------------
INSERT INTO product_variant (id, product_id, size, stock) VALUES
                                                              (1, 1, 'S', 20), (2, 1, 'M', 25), (3, 1, 'L', 15),
                                                              (4, 2, 'S', 10), (5, 2, 'M', 18), (6, 2, 'L', 12),
                                                              (7, 3, 'S', 14), (8, 3, 'M', 16), (9, 3, 'L', 9),
                                                              (10, 4, 'S', 8), (11, 4, 'M', 11), (12, 4, 'L', 7),
                                                              (13, 5, 'S', 13), (14, 5, 'M', 20), (15, 5, 'L', 10),
                                                              (16, 6, 'S', 15), (17, 6, 'M', 17), (18, 6, 'L', 9),
                                                              (19, 7, '39', 12), (20, 7, '40', 14), (21, 7, '41', 6),
                                                              (22, 8, '39', 8), (23, 8, '40', 10), (24, 8, '41', 5),
                                                              (25, 9, '39', 10), (26, 9, '40', 14), (27, 9, '41', 8),
                                                              (28, 10, '39', 11), (29, 10, '40', 13), (30, 10, '41', 7),
                                                              (31, 11, 'S', 12), (32, 11, 'M', 16), (33, 11, 'L', 9),
                                                              (34, 12, 'S', 14), (35, 12, 'M', 19), (36, 12, 'L', 10),
                                                              (37, 13, 'S', 15), (38, 13, 'M', 20), (39, 13, 'L', 11),
                                                              (40, 14, 'S', 10), (41, 14, 'M', 14), (42, 14, 'L', 8),
                                                              (43, 15, 'S', 9), (44, 15, 'M', 13), (45, 15, 'L', 7),
                                                              (46, 16, 'S', 12), (47, 16, 'M', 17), (48, 16, 'L', 9),
                                                              (49, 17, 'S', 8), (50, 17, 'M', 11), (51, 17, 'L', 6),
                                                              (52, 18, 'S', 10), (53, 18, 'M', 15), (54, 18, 'L', 8),
                                                              (55, 19, 'S', 7), (56, 19, 'M', 10), (57, 19, 'L', 5),
                                                              (58, 20, 'S', 13), (59, 20, 'M', 16), (60, 20, 'L', 9),
                                                              (61, 21, 'S', 11), (62, 21, 'M', 15), (63, 21, 'L', 8),
                                                              (64, 22, 'S', 14), (65, 22, 'M', 18), (66, 22, 'L', 10),
                                                              (67, 23, 'S', 12), (68, 23, 'M', 16), (69, 23, 'L', 9),
                                                              (70, 24, 'S', 15), (71, 24, 'M', 19), (72, 24, 'L', 10),
                                                              (73, 25, 'S', 9), (74, 25, 'M', 13), (75, 25, 'L', 7),
                                                              (76, 26, 'S', 10), (77, 26, 'M', 14), (78, 26, 'L', 8),
                                                              (79, 27, 'S', 16), (80, 27, 'M', 20), (81, 27, 'L', 11),
                                                              (82, 28, 'S', 8), (83, 28, 'M', 12), (84, 28, 'L', 6),
                                                              (85, 29, 'S', 11), (86, 29, 'M', 15), (87, 29, 'L', 8),
                                                              (88, 30, 'S', 13), (89, 30, 'M', 17), (90, 30, 'L', 9),
                                                              (91, 31, '39', 10), (92, 31, '40', 14), (93, 31, '41', 8),
                                                              (94, 32, '39', 8), (95, 32, '40', 12), (96, 32, '41', 6),
                                                              (97, 33, '39', 7), (98, 33, '40', 11), (99, 33, '41', 5),
                                                              (100, 34, '39', 18), (101, 34, '40', 22), (102, 34, '41', 12),
                                                              (103, 35, '39', 6), (104, 35, '40', 9), (105, 35, '41', 4),
                                                              (106, 36, '39', 12), (107, 36, '40', 15), (108, 36, '41', 7),
                                                              (109, 37, '39', 15), (110, 37, '40', 19), (111, 37, '41', 10),
                                                              (112, 38, '39', 6), (113, 38, '40', 9), (114, 38, '41', 4),
                                                              (115, 39, '39', 13), (116, 39, '40', 17), (117, 39, '41', 9),
                                                              (118, 40, '39', 9), (119, 40, '40', 13), (120, 40, '41', 7)
    ON DUPLICATE KEY UPDATE
                         product_id = VALUES(product_id), size = VALUES(size), stock = VALUES(stock);

-- -------------------------------------------------------------
-- A demo order for Grace (user_id=2), skewed toward "Tops" so the AI
-- recommendation / order-history tools have something meaningful to
-- work with (e.g. "based on your past orders in Tops...").
-- -------------------------------------------------------------
INSERT INTO orders (id, user_id, total_amount, status, first_name, last_name, shipping_address, phone_number, delivered_at, order_date, tracking_no, delivery_person_id, delivery_proof_key) VALUES
    (1, 2, 44.80, 'DELIVERED', 'Grace', 'Tan', '123 Orchard Road, Singapore', '91234567', DATE_SUB(NOW(), INTERVAL 2 DAY), DATE_SUB(NOW(), INTERVAL 5 DAY), 'SC-TRK-000001', NULL, 'delivery-proofs/order-1-proof.jpg')
    ON DUPLICATE KEY UPDATE
                         user_id = VALUES(user_id), total_amount = VALUES(total_amount), status = VALUES(status),
                         first_name = VALUES(first_name), last_name = VALUES(last_name), shipping_address = VALUES(shipping_address),
                         phone_number = VALUES(phone_number), delivered_at = VALUES(delivered_at), order_date = VALUES(order_date),
                         tracking_no = VALUES(tracking_no), delivery_person_id = VALUES(delivery_person_id),
                         delivery_proof_key = VALUES(delivery_proof_key);

INSERT INTO order_item (id, order_id, product_variant_id, quantity, unit_price) VALUES
                                                                                    (1, 1, 2, 1, 19.90),
                                                                                    (2, 1, 8, 1, 24.90)
    ON DUPLICATE KEY UPDATE
                         order_id = VALUES(order_id), product_variant_id = VALUES(product_variant_id),
                         quantity = VALUES(quantity), unit_price = VALUES(unit_price);

INSERT INTO payment (id, order_id, payment_method, paid_at) VALUES
    (1, 1, 'CREDIT_CARD', DATE_SUB(NOW(), INTERVAL 5 DAY))
    ON DUPLICATE KEY UPDATE
                         order_id = VALUES(order_id), payment_method = VALUES(payment_method), paid_at = VALUES(paid_at);
