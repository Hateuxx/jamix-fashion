package pe.jamix.catalog.order;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/** Named CustomerOrder because ORDER is a reserved SQL word. */
@Entity
@Table(name = "orders")
public class CustomerOrder {

    public enum Status {
        /** Stock reserved; payment and delivery are still coordinated over WhatsApp. */
        PENDING,
        CONFIRMED,
        CANCELLED
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 80)
    private String customerUsername;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, length = 16)
    private Status status = Status.PENDING;

    @Column(nullable = false)
    private Instant createdAt;

    @Column(length = 500)
    private String notes;

    @ElementCollection
    @CollectionTable(name = "order_lines", joinColumns = @JoinColumn(name = "order_id"))
    @OrderColumn(name = "line_no")
    private List<OrderLine> lines = new ArrayList<>();

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal total;

    protected CustomerOrder() {
        // for JPA
    }

    public CustomerOrder(String customerUsername, String notes, List<OrderLine> lines, Instant createdAt) {
        this.customerUsername = customerUsername;
        this.notes = notes;
        this.lines = new ArrayList<>(lines);
        this.createdAt = createdAt;
        this.total = lines.stream().map(line -> line.subtotal()).reduce(BigDecimal.ZERO, (a, b) -> a.add(b));
    }

    public Long getId() {
        return id;
    }

    public String getCustomerUsername() {
        return customerUsername;
    }

    public Status getStatus() {
        return status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public String getNotes() {
        return notes;
    }

    public List<OrderLine> getLines() {
        return List.copyOf(lines);
    }

    public BigDecimal getTotal() {
        return total;
    }
}
