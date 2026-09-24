-- New catalogue photos for Vestido Rita and Vestido Stephie (same /img URLs, new images).
-- Their descriptions (used in the page's schema.org data) now match what the photos show.
-- V2 is not edited because it has already been applied: every change goes in a new migration.

UPDATE products
SET description = 'Vestido largo con escote en V, mangas tres cuartos y detalles de encaje.'
WHERE slug = 'vestido-rita';

UPDATE products
SET description = 'Vestido largo de hombros descubiertos, con vuelo de encaje y falda escalonada.'
WHERE slug = 'vestido-stephie';
