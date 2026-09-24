package pe.jamix.catalog.product;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "products")
public class Product {

    /** Wholesale pricing kicks in from this many units of the same garment. */
    public static final int WHOLESALE_MIN_UNITS = 6;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 80)
    private String slug;

    @Column(nullable = false, length = 120)
    private String name;

    // VARCHAR, not MySQL's native ENUM: adding a category must not require an ALTER TABLE.
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, length = 16)
    private Category category;

    /** Garment type shown on the card: "Tops", "Vestidos cortos", "Conjuntos"... */
    @Column(length = 60)
    private String type;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, length = 16)
    private Gender gender = Gender.UNISEX;

    @Column(length = 500)
    private String description;

    /** Retail price; for size-dependent garments this is the "from" price. */
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    /** Upper bound when the price depends on size (e.g. Vestido Túnica S/ 50–60); null otherwise. */
    @Column(precision = 10, scale = 2)
    private BigDecimal priceMax;

    @Column(precision = 10, scale = 2)
    private BigDecimal wholesalePrice;

    /** Upper bound of the wholesale price when it depends on size (e.g. S/ 32–36); null otherwise. */
    @Column(precision = 10, scale = 2)
    private BigDecimal wholesalePriceMax;

    @Column(length = 255)
    private String imageUrl;

    /** Extra photos after the main one (model, back, detail), in display order. */
    @ElementCollection
    @CollectionTable(name = "product_images", joinColumns = @JoinColumn(name = "product_id"))
    @OrderColumn(name = "sort_order")
    @Column(name = "url", nullable = false, length = 255)
    private List<String> gallery = new ArrayList<>();

    @Column(nullable = false)
    private int stock;

    /** Optimistic lock: two concurrent orders can't both take the last unit. */
    @Version
    private long version;

    protected Product() {
        // for JPA
    }

    public Product(String slug, String name, Category category, String type, Gender gender, String description,
            BigDecimal price, BigDecimal priceMax, BigDecimal wholesalePrice, BigDecimal wholesalePriceMax,
            String imageUrl, int stock) {
        apply(slug, name, category, type, gender, description, price, priceMax, wholesalePrice, wholesalePriceMax,
                imageUrl, stock);
    }

    public void apply(String slug, String name, Category category, String type, Gender gender, String description,
            BigDecimal price, BigDecimal priceMax, BigDecimal wholesalePrice, BigDecimal wholesalePriceMax,
            String imageUrl, int stock) {
        this.slug = slug;
        this.name = name;
        this.category = category;
        this.type = type;
        this.gender = gender == null ? Gender.UNISEX : gender;
        this.description = description;
        this.price = price;
        this.priceMax = priceMax;
        this.wholesalePrice = wholesalePrice;
        this.wholesalePriceMax = wholesalePriceMax;
        this.imageUrl = imageUrl;
        setStock(stock);
    }

    public void setStock(int stock) {
        if (stock < 0) {
            throw new IllegalArgumentException("stock must be >= 0");
        }
        this.stock = stock;
    }

    public boolean hasStock(int quantity) {
        return stock >= quantity;
    }

    public void decreaseStock(int quantity) {
        if (!hasStock(quantity)) {
            throw new IllegalStateException("Not enough stock for " + slug);
        }
        this.stock -= quantity;
    }

    /** Unit price for an order line: wholesale from {@link #WHOLESALE_MIN_UNITS} units when a wholesale price exists. */
    public BigDecimal unitPriceFor(int quantity) {
        if (wholesalePrice != null && quantity >= WHOLESALE_MIN_UNITS) {
            return wholesalePrice;
        }
        return price;
    }

    public Long getId() {
        return id;
    }

    public String getSlug() {
        return slug;
    }

    public String getName() {
        return name;
    }

    public Category getCategory() {
        return category;
    }

    public String getType() {
        return type;
    }

    public Gender getGender() {
        return gender;
    }

    public BigDecimal getWholesalePriceMax() {
        return wholesalePriceMax;
    }

    public String getDescription() {
        return description;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public BigDecimal getPriceMax() {
        return priceMax;
    }

    public BigDecimal getWholesalePrice() {
        return wholesalePrice;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public List<String> getGallery() {
        return List.copyOf(gallery);
    }

    public int getStock() {
        return stock;
    }

    public long getVersion() {
        return version;
    }
}
