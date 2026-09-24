package pe.jamix.catalog.product;

import java.math.BigDecimal;
import java.util.List;

/**
 * Public catalog view. Exposes availability, never the exact stock count.
 */
public record ProductResponse(
        Long id,
        String slug,
        String name,
        String category,
        String categoryLabel,
        String type,
        /* femenino | masculino | unisex: which family members the store offers for this garment. */
        String gender,
        String description,
        BigDecimal price,
        BigDecimal priceMax,
        String priceLabel,
        BigDecimal wholesalePrice,
        BigDecimal wholesalePriceMax,
        String wholesaleLabel,
        int wholesaleMinUnits,
        String imageUrl,
        /* Extra photos after imageUrl, for the quick-view gallery. */
        List<String> gallery,
        boolean inStock) {

    public static ProductResponse from(Product p) {
        return new ProductResponse(
                p.getId(),
                p.getSlug(),
                p.getName(),
                p.getCategory().slug(),
                p.getCategory().label(),
                p.getType(),
                p.getGender().slug(),
                p.getDescription(),
                p.getPrice(),
                p.getPriceMax(),
                priceLabel(p.getPrice(), p.getPriceMax()),
                p.getWholesalePrice(),
                p.getWholesalePriceMax(),
                p.getWholesalePrice() == null ? null : priceLabel(p.getWholesalePrice(), p.getWholesalePriceMax()),
                Product.WHOLESALE_MIN_UNITS,
                p.getImageUrl(),
                p.getGallery(),
                p.getStock() > 0);
    }

    /** "S/ 45" or, for size-dependent prices, "S/ 50 – 60". */
    static String priceLabel(BigDecimal price, BigDecimal priceMax) {
        String from = "S/ " + plain(price);
        if (priceMax == null || priceMax.compareTo(price) == 0) {
            return from;
        }
        return from + " – " + plain(priceMax);
    }

    private static String plain(BigDecimal value) {
        return value.stripTrailingZeros().toPlainString();
    }
}
