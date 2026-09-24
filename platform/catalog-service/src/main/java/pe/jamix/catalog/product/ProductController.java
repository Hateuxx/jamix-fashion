package pe.jamix.catalog.product;

import java.util.List;
import java.util.concurrent.TimeUnit;

import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import pe.jamix.catalog.common.NotFoundException;

/**
 * Public, read-only catalog consumed by the React storefront (and any other client).
 *
 * <pre>
 * GET /api/v1/products                 all products
 * GET /api/v1/products?category=ninos  filtered (ella | el | ninos)
 * GET /api/v1/products/{slug}          single product
 * </pre>
 */
@RestController
@RequestMapping("/api/v1/products")
public class ProductController {

    private static final CacheControl PUBLIC_CACHE = CacheControl.maxAge(60, TimeUnit.SECONDS).cachePublic();

    private final ProductService service;

    public ProductController(ProductService service) {
        this.service = service;
    }

    @GetMapping
    public ResponseEntity<List<ProductResponse>> list(@RequestParam(required = false) String category) {
        var filter = Category.fromSlug(category);
        if (category != null && !category.isBlank() && filter.isEmpty()) {
            throw new NotFoundException("Categoría desconocida '" + category + "'. Usa ella, el o ninos.");
        }
        return ResponseEntity.ok().cacheControl(PUBLIC_CACHE).body(service.catalog(filter));
    }

    @GetMapping("/{slug}")
    public ResponseEntity<ProductResponse> get(@PathVariable String slug) {
        return ResponseEntity.ok().cacheControl(PUBLIC_CACHE).body(service.bySlug(slug));
    }
}
