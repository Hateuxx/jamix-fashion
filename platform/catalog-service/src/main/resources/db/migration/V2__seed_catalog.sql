-- Initial catalogue. Sources: the editorial catalogue brief (first block) and the product grid of
-- the current site, HTML/index.html. Stock figures are placeholders: set the real ones in the admin.
-- To change products from now on, use the admin panel or a new migration, not this file.

INSERT INTO products (slug, name, category, type, description, price, price_max, wholesale_price, wholesale_price_max, image_url, stock) VALUES
-- Editorial catalogue brief
('falda-bobo',      'Falda Bobo',           'ELLA',  'Faldas',           'Falda con vuelos en capas. Ligera y con movimiento para books y gender reveals.', 25, NULL, 18, NULL, '/img/falda-bobo.jpeg',      24),
('vestido-rita',    'Vestido Rita',         'ELLA',  'Vestidos largos',  'Vestido midi de tiras con caída suave, pensado para la foto familiar.',        45, NULL, 33, NULL, '/img/vestido-rita.jpeg',    12),
('vestido-stephie', 'Vestido Stephie',      'ELLA',  'Vestidos largos',  'Vestido largo con escote cuadrado y falda amplia.',                            50, NULL, 35, NULL, '/img/vestido-stephie.jpeg', 10),
('vestido-tunica',  'Vestido Túnica',       'NINOS', 'Vestidos de niña', 'Túnica de niña en blanco. El precio varía según la talla.',                    50,   60, 32,   36, '/img/vestido-tunica.jpeg',  15),
('conjunto-walter', 'Conjunto Niño Walter', 'NINOS', 'Conjuntos',        'Camisa y short a juego para que el niño combine con papá.',                    65, NULL, 32,   36, '/img/conjunto-walter.jpeg',  8),
('short-bolsillo',  'Short Bolsillo',       'EL',    'Shorts',           'Short blanco con bolsillos para sesiones en playa o campo.',                   52, NULL, 32, NULL, '/img/short-bolsillo.jpeg',   0),
-- Ella: tops
('top-marleny',   'Top Marleny',   'ELLA', 'Tops', NULL, 42, NULL, 18, NULL, '/img/top-marleny.jpeg',   10),
('top-naty',      'Top Naty',      'ELLA', 'Tops', NULL, 29, NULL, 18, NULL, '/img/top-naty.jpeg',      10),
('top-maricielo', 'Top Maricielo', 'ELLA', 'Tops', NULL, 25, NULL, 18, NULL, '/img/top-maricielo.jpeg', 10),
('top-lili',      'Top Lili',      'ELLA', 'Tops', NULL, 29, NULL, 18, NULL, '/img/top-lili.jpeg',      10),
('top-lazo',      'Top Lazo',      'ELLA', 'Tops', NULL, 32, NULL, 18, NULL, '/img/top-lazo.jpeg',      10),
('top-juana',     'Top Juana',     'ELLA', 'Tops', NULL, 31, NULL, 18, NULL, '/img/top-juana.jpeg',     10),
('top-danny',     'Top Danny',     'ELLA', 'Tops', NULL, 29, NULL, 18, NULL, '/img/top-danny.jpeg',     10),
-- Ella: faldas y pantalones
('falda-malla',     'Falda Malla',     'ELLA', 'Faldas',              NULL, 52, NULL, 18, NULL, '/img/falda-malla.jpeg',     10),
('falda-caro',      'Falda Caro',      'ELLA', 'Faldas',              NULL, 25, NULL, 18, NULL, '/img/falda-caro.jpeg',      10),
('falda-lola',      'Falda Lola',      'ELLA', 'Faldas',              NULL, 52, NULL, 33, NULL, '/img/falda-lola.jpeg',      10),
('falda-adriana',   'Falda Adriana',   'ELLA', 'Faldas',              NULL, 32, NULL, 18, NULL, '/img/falda-adriana.jpeg',   10),
('pantalon-palazo', 'Pantalón Palazo', 'ELLA', 'Pantalones de mujer', NULL, 52, NULL, 30, NULL, '/img/pantalon-palazo.jpeg', 10),
('pantalon-silvia', 'Pantalón Silvia', 'ELLA', 'Pantalones de mujer', NULL, 42, NULL, 30, NULL, '/img/pantalon-silvia.jpeg', 10),
-- Ella: vestidos
('vestido-corset',   'Vestido Corset',   'ELLA', 'Vestidos cortos', NULL, 54, NULL, 35, NULL, '/img/vestido-corset.jpeg',   10),
('vestido-estrella', 'Vestido Estrella', 'ELLA', 'Vestidos cortos', NULL, 50, NULL, 35, NULL, '/img/vestido-estrella.jpeg', 10),
('vestido-carmen',   'Vestido Carmen',   'ELLA', 'Vestidos cortos', NULL, 50, NULL, 35, NULL, '/img/vestido-carmen.jpeg',   10),
('vestido-jenny',    'Vestido Jenny',    'ELLA', 'Vestidos cortos', NULL, 50, NULL, 35, NULL, '/img/vestido-jenny.jpeg',    10),
('vestido-lua',      'Vestido Lua',      'ELLA', 'Vestidos cortos', NULL, 49, NULL, 35, NULL, '/img/vestido-lua.jpeg',      10),
('vestido-rocio',    'Vestido Rocío',    'ELLA', 'Vestidos cortos', NULL, 49, NULL, 35, NULL, '/img/vestido-rocio.jpeg',    10),
('vestido-rumbera',  'Vestido Rumbera',  'ELLA', 'Vestidos cortos', NULL, 49, NULL, 35, NULL, '/img/vestido-rumbera.jpeg',  10),
('vestido-piramide', 'Vestido Pirámide', 'ELLA', 'Vestidos largos', NULL, 60, NULL, 35, NULL, '/img/vestido-piramide.jpeg', 10),
('vestido-roxi',     'Vestido Roxi',     'ELLA', 'Vestidos largos', NULL, 58, NULL, 35, NULL, '/img/vestido-roxi.jpeg',     10),
('vestido-sofia',    'Vestido Sofía',    'ELLA', 'Vestidos largos', NULL, 58, NULL, 35, NULL, '/img/vestido-sofia.jpeg',    10),
-- Él
('conjunto-andres', 'Conjunto Andrés', 'EL', 'Conjuntos', NULL, 104, NULL, 35, NULL, '/img/conjunto-andres.jpeg', 10),
-- Niños
('conjunto-andres-short', 'Conjunto Andrés Short', 'NINOS', 'Conjuntos',        NULL, 65, NULL, 32, 36, '/img/conjunto-andres-short.jpeg', 10),
('vestido-angela',        'Vestido Ángela',        'NINOS', 'Vestidos de niña', NULL, 51, NULL, 32, 36, '/img/vestido-angela.jpeg',        10),
('vestido-fernandita',    'Vestido Fernandita',    'NINOS', 'Vestidos de niña', NULL, 60, NULL, 33, 38, '/img/vestido-fernandita.jpeg',    10),
('vestido-rumbera-nina',  'Vestido Rumbera Niña',  'NINOS', 'Vestidos de niña', NULL, 60, NULL, 40, 60, '/img/vestido-rumbera-nina.jpeg',  10),
('vestido-nicol',         'Vestido Nicol',         'NINOS', 'Vestidos de niña', NULL, 60, NULL, 40, 60, '/img/vestido-nicol.jpeg',         10);
