-- Extra photos per product (model, back, detail) shown in the storefront's quick view.
-- products.image_url stays the main photo; these follow it in sort_order.

CREATE TABLE product_images (
    product_id BIGINT       NOT NULL,
    sort_order INT          NOT NULL,
    url        VARCHAR(255) NOT NULL,
    CONSTRAINT pk_product_images PRIMARY KEY (product_id, sort_order),
    CONSTRAINT fk_product_images_product FOREIGN KEY (product_id) REFERENCES products (id) ON DELETE CASCADE
);

INSERT INTO product_images (product_id, sort_order, url)
SELECT id, 0, '/img/falda-bobo-look-1.jpeg' FROM products WHERE slug = 'falda-bobo';
INSERT INTO product_images (product_id, sort_order, url)
SELECT id, 1, '/img/falda-bobo-look-2.jpeg' FROM products WHERE slug = 'falda-bobo';

INSERT INTO product_images (product_id, sort_order, url)
SELECT id, 0, '/img/vestido-stephie-modelo.jpeg' FROM products WHERE slug = 'vestido-stephie';
INSERT INTO product_images (product_id, sort_order, url)
SELECT id, 1, '/img/vestido-stephie-detalle.jpeg' FROM products WHERE slug = 'vestido-stephie';

INSERT INTO product_images (product_id, sort_order, url)
SELECT id, 0, '/img/vestido-corset-modelo.jpeg' FROM products WHERE slug = 'vestido-corset';
