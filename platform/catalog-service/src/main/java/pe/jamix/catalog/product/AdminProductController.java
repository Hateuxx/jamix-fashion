package pe.jamix.catalog.product;

import java.net.URI;
import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Product and stock management for the admin micro-frontend. Reading is open to all staff;
 * changes to products and physical counts need MANAGER or OWNER (see SecurityConfig).
 */
@RestController
@RequestMapping("/api/v1/admin/products")
public class AdminProductController {

    /** Physical count: the stock found on the shelf, plus the version the admin was looking at. */
    public record StockUpdate(@NotNull @PositiveOrZero Integer stock, @NotNull Long version, @Size(max = 200) String note) {
    }

    private final ProductService service;

    public AdminProductController(ProductService service) {
        this.service = service;
    }

    @GetMapping
    public List<AdminProductResponse> inventory() {
        return service.inventory();
    }

    @PostMapping
    public ResponseEntity<AdminProductResponse> create(@Valid @RequestBody ProductRequest request,
            Authentication authentication) {
        AdminProductResponse created = service.create(request, authentication.getName());
        return ResponseEntity.created(URI.create("/api/v1/admin/products/" + created.id())).body(created);
    }

    @PutMapping("/{id}")
    public AdminProductResponse update(@PathVariable long id, @Valid @RequestBody ProductRequest request,
            Authentication authentication) {
        return service.update(id, request, authentication.getName());
    }

    @PatchMapping("/{id}/stock")
    public AdminProductResponse updateStock(@PathVariable long id, @Valid @RequestBody StockUpdate body,
            Authentication authentication) {
        return service.updateStock(id, body.stock(), body.version(), body.note(), authentication.getName());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
