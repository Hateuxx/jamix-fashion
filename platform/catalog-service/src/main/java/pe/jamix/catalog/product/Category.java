package pe.jamix.catalog.product;

import java.util.Optional;

public enum Category {
    ELLA("ella", "Ella"),
    EL("el", "Él"),
    NINOS("ninos", "Niños");

    private final String slug;
    private final String label;

    Category(String slug, String label) {
        this.slug = slug;
        this.label = label;
    }

    public String slug() {
        return slug;
    }

    public String label() {
        return label;
    }

    /** Resolves the URL-friendly slug used by filters ({@code ?categoria=ninos}). Unknown values yield empty. */
    public static Optional<Category> fromSlug(String value) {
        if (value == null || value.isBlank()) {
            return Optional.empty();
        }
        for (Category c : values()) {
            if (c.slug.equalsIgnoreCase(value) || c.name().equalsIgnoreCase(value)) {
                return Optional.of(c);
            }
        }
        return Optional.empty();
    }
}
