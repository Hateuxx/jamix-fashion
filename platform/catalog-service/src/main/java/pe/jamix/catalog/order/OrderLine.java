package pe.jamix.catalog.order;

import java.math.BigDecimal;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

/** Snapshot of a product at order time, so later price edits don't rewrite history. */
@Embeddable
public class OrderLine {

    @Column(nullable = false)
    private Long productId;

    @Column(nullable = false, length = 120)
    private String productName;

    @Column(nullable = false)
    private int quantity;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal unitPrice;

    @Column(nullable = false)
    private boolean wholesale;

    protected OrderLine() {
        // for JPA
    }

    public OrderLine(Long productId, String productName, int quantity, BigDecimal unitPrice, boolean wholesale) {
        this.productId = productId;
        this.productName = productName;
        this.quantity = quantity;
        this.unitPrice = unitPrice;
        this.wholesale = wholesale;
    }

    public BigDecimal subtotal() {
        return unitPrice.multiply(BigDecimal.valueOf(quantity));
    }

    public Long getProductId() {
        return productId;
    }

    public String getProductName() {
        return productName;
    }

    public int getQuantity() {
        return quantity;
    }

    public BigDecimal getUnitPrice() {
        return unitPrice;
    }

    public boolean isWholesale() {
        return wholesale;
    }
}
