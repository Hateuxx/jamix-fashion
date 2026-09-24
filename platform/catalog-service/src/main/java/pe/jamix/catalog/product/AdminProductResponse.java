package pe.jamix.catalog.product;

import java.math.BigDecimal;

/**
 * Back-office view: includes the exact stock and the lock version the admin panel echoes back.
 */
public record AdminProductResponse(
        Long id,
        String slug,
        String name,
        Category category,
        String type,
        Gender gender,
        String description,
        BigDecimal price,
        BigDecimal priceMax,
        BigDecimal wholesalePrice,
        BigDecimal wholesalePriceMax,
        String imageUrl,
        int stock,
        long version) {

    public static AdminProductResponse from(Product p) {
        return new AdminProductResponse(p.getId(), p.getSlug(), p.getName(), p.getCategory(), p.getType(),
                p.getGender(), p.getDescription(), p.getPrice(), p.getPriceMax(), p.getWholesalePrice(), p.getWholesalePriceMax(),
                p.getImageUrl(), p.getStock(), p.getVersion());
    }
}
