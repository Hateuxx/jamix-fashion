package pe.jamix.catalog.stock;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/** One line of the stock ledger (kardex). Immutable once written. */
@Entity
@Table(name = "stock_movements")
public class StockMovement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long productId;

    /** Snapshot, so the history stays readable even if the product is renamed. */
    @Column(nullable = false, length = 120)
    private String productName;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, length = 20)
    private MovementType type;

    /** Signed change: +3 received, -1 sold. */
    @Column(nullable = false)
    private int quantity;

    @Column(nullable = false)
    private int stockAfter;

    @Column(length = 200)
    private String note;

    @Column(nullable = false, length = 40)
    private String username;

    @Column(nullable = false)
    private Instant createdAt;

    protected StockMovement() {
        // for JPA
    }

    public StockMovement(Long productId, String productName, MovementType type, int quantity, int stockAfter,
            String note, String username, Instant createdAt) {
        this.productId = productId;
        this.productName = productName;
        this.type = type;
        this.quantity = quantity;
        this.stockAfter = stockAfter;
        this.note = note;
        this.username = username;
        this.createdAt = createdAt;
    }

    public Long getId() {
        return id;
    }

    public Long getProductId() {
        return productId;
    }

    public String getProductName() {
        return productName;
    }

    public MovementType getType() {
        return type;
    }

    public int getQuantity() {
        return quantity;
    }

    public int getStockAfter() {
        return stockAfter;
    }

    public String getNote() {
        return note;
    }

    public String getUsername() {
        return username;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
