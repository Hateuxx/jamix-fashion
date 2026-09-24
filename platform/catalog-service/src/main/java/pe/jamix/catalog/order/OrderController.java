package pe.jamix.catalog.order;

import java.net.URI;
import java.util.List;

import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import pe.jamix.catalog.order.OrderDtos.OrderRequest;
import pe.jamix.catalog.order.OrderDtos.OrderResponse;

/** Requires a valid JWT with role CUSTOMER or ADMIN (see SecurityConfig). */
@RestController
@RequestMapping("/api/v1/orders")
public class OrderController {

    private final OrderService service;

    public OrderController(OrderService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<OrderResponse> place(@AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody OrderRequest request) {
        OrderResponse order = service.place(jwt.getSubject(), request);
        return ResponseEntity.created(URI.create("/api/v1/orders/" + order.id())).body(order);
    }

    @GetMapping("/mine")
    public List<OrderResponse> mine(@AuthenticationPrincipal Jwt jwt) {
        return service.history(jwt.getSubject());
    }
}
