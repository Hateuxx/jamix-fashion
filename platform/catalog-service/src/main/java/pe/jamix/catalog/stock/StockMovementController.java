package pe.jamix.catalog.stock;

import java.util.List;

import jakarta.validation.Valid;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import pe.jamix.catalog.stock.StockService.MovementRequest;
import pe.jamix.catalog.stock.StockService.MovementResponse;
import pe.jamix.catalog.stock.StockService.MovementResult;
import pe.jamix.catalog.stock.StockService.MovementTypeInfo;
import pe.jamix.catalog.user.Role;

/**
 * Stock ledger for the admin panel. Any staff role may read it and register movements; which
 * movement types each role may register is enforced in StockService.
 */
@RestController
@RequestMapping("/api/v1/admin")
public class StockMovementController {

    private final StockService stock;

    public StockMovementController(StockService stock) {
        this.stock = stock;
    }

    @PostMapping("/products/{id}/movements")
    public MovementResult register(@PathVariable long id, @Valid @RequestBody MovementRequest request,
            Authentication authentication) {
        return stock.register(id, request, authentication.getName(), Role.from(authentication.getAuthorities()));
    }

    @GetMapping("/products/{id}/movements")
    public List<MovementResponse> history(@PathVariable long id, @RequestParam(defaultValue = "50") int limit) {
        return stock.history(id, limit);
    }

    @GetMapping("/movements")
    public List<MovementResponse> recent(@RequestParam(defaultValue = "100") int limit) {
        return stock.recent(limit);
    }

    @GetMapping("/movement-types")
    public List<MovementTypeInfo> types(Authentication authentication) {
        return stock.typesFor(Role.from(authentication.getAuthorities()));
    }
}
