-- Retail prices from "JAMIX · Precios a reemplazar" (25 Sep 2026): 44.7 % margin over the supplier's
-- wholesale cost. Wholesale prices (6+ units) are not part of that list and stay as they are.
-- Garments the list names but the catalogue doesn't sell (Camisa Andrés sola, Top Mary, the
-- Top + pantalón sets) are not added here.

UPDATE products SET price = 60.00 WHERE slug = 'falda-lola';       -- 52 -> 60
UPDATE products SET price = 58.00 WHERE slug = 'short-bolsillo';   -- 52 -> 58
UPDATE products SET price = 60.00 WHERE slug = 'vestido-rita';     -- 45 -> 60
UPDATE products SET price = 60.00 WHERE slug = 'vestido-roxi';     -- 58 -> 60
UPDATE products SET price = 60.00 WHERE slug = 'vestido-sofia';    -- 58 -> 60
UPDATE products SET price = 54.00 WHERE slug = 'pantalon-palazo';  -- 52 -> 54
UPDATE products SET price = 54.00 WHERE slug = 'falda-malla';      -- 52 -> 54
UPDATE products SET price = 33.00 WHERE slug = 'top-lazo';         -- 32 -> 33
UPDATE products SET price = 33.00 WHERE slug = 'falda-adriana';    -- 32 -> 33

-- Set: S/ 119 (41 % margin) instead of 127, so it costs S/ 7 less than shirt + trousers bought
-- separately (63 + 63) and gives a reason to take the whole set.
UPDATE products SET price = 119.00 WHERE slug = 'conjunto-andres'; -- 104 -> 119

-- Listed as already correct at S/ 63 in the same document; the catalogue still had 50.
UPDATE products SET price = 63.00 WHERE slug = 'vestido-stephie';  -- 50 -> 63
