package pe.jamix.catalog.stock;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import pe.jamix.catalog.common.ConflictException;
import pe.jamix.catalog.common.NotFoundException;
import pe.jamix.catalog.product.AdminProductResponse;
import pe.jamix.catalog.product.Product;
import pe.jamix.catalog.product.ProductRepository;
import pe.jamix.catalog.user.Role;

/**
 * The only way stock changes: every change is applied to the product AND written to the ledger in
 * the same transaction, with who did it and why.
 */
@Service
public class StockService {

    public record MovementRequest(@NotNull MovementType type, @Min(1) @Max(999) int quantity,
            @Size(max = 200) String note) {
    }

    public record MovementResponse(Long id, Long productId, String productName, String type, String typeLabel,
            int quantity, int stockAfter, String note, String username, Instant createdAt) {

        static MovementResponse from(StockMovement m) {
            return new MovementResponse(m.getId(), m.getProductId(), m.getProductName(), m.getType().name(),
                    m.getType().label(), m.getQuantity(), m.getStockAfter(), m.getNote(), m.getUsername(),
                    m.getCreatedAt());
        }
    }

    /** Result of a manual movement: the ledger line plus the product as it is now (new stock and version). */
    public record MovementResult(MovementResponse movement, AdminProductResponse product) {
    }

    public record MovementTypeInfo(String type, String label, int sign, boolean allowed) {
    }

    private final ProductRepository products;
    private final StockMovementRepository movements;

    public StockService(ProductRepository products, StockMovementRepository movements) {
        this.products = products;
        this.movements = movements;
    }

    /**
     * Applies a signed change and records it. Callers run inside their own transaction; the stock
     * never goes below zero and the product's @Version protects against concurrent edits.
     */
    @Transactional
    public StockMovement record(Product product, MovementType type, int delta, String note, String username) {
        int after = product.getStock() + delta;
        if (after < 0) {
            throw new ConflictException("Stock insuficiente de " + product.getName() + ": quedan "
                    + product.getStock() + ".");
        }
        product.setStock(after);
        String cleanNote = note == null || note.isBlank() ? null : note.trim();
        return movements.save(new StockMovement(product.getId(), product.getName(), type, delta, after, cleanNote,
                username, Instant.now()));
    }

    /** A movement registered by hand from the admin panel (+1, "venta en tienda", "ingreso"...). */
    @Transactional
    public MovementResult register(long productId, MovementRequest request, String username, Role role) {
        MovementType type = request.type();
        if (!type.isManual()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Ese tipo de movimiento no se registra a mano.");
        }
        if (!type.allowedFor(role)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Tu rol no puede registrar: " + type.label() + ".");
        }
        Product product = products.findById(productId)
                .orElseThrow(() -> new NotFoundException("No existe el producto " + productId + "."));
        StockMovement movement = record(product, type, type.sign() * request.quantity(), request.note(), username);
        Product saved = products.saveAndFlush(product);
        return new MovementResult(MovementResponse.from(movement), AdminProductResponse.from(saved));
    }

    @Transactional(readOnly = true)
    public List<MovementResponse> history(long productId, int limit) {
        return movements.findByProductIdOrderByCreatedAtDescIdDesc(productId, PageRequest.of(0, clamp(limit))).stream()
                .map(MovementResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<MovementResponse> recent(int limit) {
        return movements.findAllByOrderByCreatedAtDescIdDesc(PageRequest.of(0, clamp(limit))).stream()
                .map(MovementResponse::from)
                .toList();
    }

    /** Movement types with whether the current role may register them (drives the admin UI). */
    public List<MovementTypeInfo> typesFor(Role role) {
        // A plain loop: enum constants are never null, so a stream with a method reference (or a null
        // check inside a lambda) only adds noise for the null-analysis in the IDE.
        List<MovementTypeInfo> types = new ArrayList<>();
        for (MovementType type : MovementType.values()) {
            if (type.isManual()) {
                types.add(new MovementTypeInfo(type.name(), type.label(), type.sign(), type.allowedFor(role)));
            }
        }
        return types;
    }

    private static int clamp(int limit) {
        return Math.max(1, Math.min(200, limit));
    }
}
