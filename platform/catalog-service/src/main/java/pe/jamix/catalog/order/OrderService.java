package pe.jamix.catalog.order;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import pe.jamix.catalog.common.ConflictException;
import pe.jamix.catalog.common.NotFoundException;
import pe.jamix.catalog.order.OrderDtos.Item;
import pe.jamix.catalog.order.OrderDtos.OrderRequest;
import pe.jamix.catalog.order.OrderDtos.OrderResponse;
import pe.jamix.catalog.product.Product;
import pe.jamix.catalog.product.ProductRepository;
import pe.jamix.catalog.stock.MovementType;
import pe.jamix.catalog.stock.StockService;

@Service
public class OrderService {

    private final OrderRepository orders;
    private final ProductRepository products;
    private final StockService stock;
    private final Clock clock;

    public OrderService(OrderRepository orders, ProductRepository products, StockService stock) {
        this.orders = orders;
        this.products = products;
        this.stock = stock;
        this.clock = Clock.systemUTC();
    }

    /**
     * Reserves stock and records the order. The whole order fails if any line lacks stock;
     * concurrent orders for the same product are serialized by the product's @Version.
     */
    @Transactional
    public OrderResponse place(String username, OrderRequest request) {
        // Merge repeated product IDs so the wholesale threshold and the stock check see the real quantity.
        Map<Long, Integer> quantities = new LinkedHashMap<>();
        for (Item item : request.items()) {
            quantities.merge(item.productId(), item.quantity(), (a, b) -> a + b);
        }

        // 1. Check every line first: the whole order fails if one of them lacks stock.
        List<OrderLine> lines = new ArrayList<>();
        Map<Product, Integer> reserved = new LinkedHashMap<>();
        quantities.forEach((productId, quantity) -> {
            Product product = products.findById(productId)
                    .orElseThrow(() -> new NotFoundException("No existe el producto " + productId + "."));
            if (!product.hasStock(quantity)) {
                throw new ConflictException("Stock insuficiente para " + product.getName()
                        + ": quedan " + product.getStock() + ".");
            }
            var unitPrice = product.unitPriceFor(quantity);
            boolean wholesale = unitPrice.compareTo(product.getPrice()) != 0;
            lines.add(new OrderLine(product.getId(), product.getName(), quantity, unitPrice, wholesale));
            reserved.put(product, quantity);
        });

        // 2. Save the order, then take the stock through the ledger so each sale shows who and which order.
        CustomerOrder saved = orders.save(new CustomerOrder(username, request.notes(), lines, Instant.now(clock)));
        reserved.forEach((product, quantity) ->
                stock.record(product, MovementType.VENTA_WEB, -quantity, "Pedido web #" + saved.getId(), username));
        return OrderResponse.from(saved);
    }

    @Transactional(readOnly = true)
    public List<OrderResponse> history(String username) {
        return orders.findByCustomerUsernameOrderByCreatedAtDesc(username).stream()
                .map(OrderResponse::from)
                .toList();
    }
}
