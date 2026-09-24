package pe.jamix.catalog.order;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public final class OrderDtos {

    private OrderDtos() {
    }

    public record OrderRequest(
            @NotEmpty @Size(max = 50) List<@Valid @NotNull Item> items,
            @Size(max = 500) String notes) {
    }

    public record Item(@NotNull Long productId, @Min(1) @Max(500) int quantity) {
    }

    public record LineResponse(Long productId, String productName, int quantity, BigDecimal unitPrice,
            boolean wholesale, BigDecimal subtotal) {

        static LineResponse from(OrderLine line) {
            return new LineResponse(line.getProductId(), line.getProductName(), line.getQuantity(),
                    line.getUnitPrice(), line.isWholesale(), line.subtotal());
        }
    }

    public record OrderResponse(Long id, String status, Instant createdAt, List<LineResponse> lines,
            BigDecimal total, String notes) {

        static OrderResponse from(CustomerOrder order) {
            return new OrderResponse(order.getId(), order.getStatus().name(), order.getCreatedAt(),
                    order.getLines().stream().map(LineResponse::from).toList(), order.getTotal(), order.getNotes());
        }
    }
}
