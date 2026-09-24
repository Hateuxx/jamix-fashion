-- Who each garment is for, so the store only offers matching family members in "¿Para quién es?".
-- Category alone isn't enough: "Niños" mixes girls' dresses with boys' sets.

ALTER TABLE products ADD COLUMN gender VARCHAR(16) NOT NULL DEFAULT 'UNISEX';

UPDATE products SET gender = 'FEMENINO' WHERE category = 'ELLA';
UPDATE products SET gender = 'MASCULINO' WHERE category = 'EL';
UPDATE products SET gender = 'FEMENINO' WHERE category = 'NINOS' AND type = 'Vestidos de niña';
UPDATE products SET gender = 'MASCULINO'
WHERE slug IN ('conjunto-walter', 'conjunto-andres-short');
