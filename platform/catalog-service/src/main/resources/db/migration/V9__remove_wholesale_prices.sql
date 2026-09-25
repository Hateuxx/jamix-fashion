-- Jamix no longer sells wholesale (6+ units): every order is charged the retail price.
-- Without a wholesale price, Product.unitPriceFor() always returns the retail price and the store
-- and the SSR page show no "Mayorista" label. Previous values remain in V2 if they are ever needed.

UPDATE products SET wholesale_price = NULL, wholesale_price_max = NULL;
