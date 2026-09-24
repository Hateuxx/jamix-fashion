package pe.jamix.catalog.product;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import pe.jamix.catalog.common.ConflictException;
import pe.jamix.catalog.common.NotFoundException;
import pe.jamix.catalog.stock.MovementType;
import pe.jamix.catalog.stock.StockService;

@Service
@Transactional(readOnly = true)
public class ProductService {

    private final ProductRepository products;
    private final StockService stock;

    public ProductService(ProductRepository products, StockService stock) {
        this.products = products;
        this.stock = stock;
    }

    /** Grouped Ella → Él → Niños (enum order, not alphabetical), then in insertion order. */
    public List<ProductResponse> catalog(Optional<Category> category) {
        List<Product> result = category
                .map(products::findByCategoryOrderByIdAsc)
                .orElseGet(products::findAllByOrderByIdAsc);
        return result.stream()
                .sorted(Comparator.comparing(product -> product.getCategory()))
                .map(ProductResponse::from)
                .toList();
    }

    public ProductResponse bySlug(String slug) {
        return products.findBySlug(slug)
                .map(ProductResponse::from)
                .orElseThrow(() -> new NotFoundException("No existe el producto '" + slug + "'."));
    }

    public List<AdminProductResponse> inventory() {
        return products.findAllByOrderByIdAsc().stream().map(AdminProductResponse::from).toList();
    }

    /** The initial stock enters the ledger as an INGRESO, like any later delivery. */
    @Transactional
    public AdminProductResponse create(ProductRequest req, String username) {
        if (products.existsBySlug(req.slug())) {
            throw new ConflictException("Ya existe un producto con el slug '" + req.slug() + "'.");
        }
        Product product = products.save(new Product(req.slug(), req.name(), req.category(), req.type(), req.gender(),
                req.description(), req.price(), req.priceMax(), req.wholesalePrice(), req.wholesalePriceMax(),
                req.imageUrl(), 0));
        if (req.stock() > 0) {
            stock.record(product, MovementType.INGRESO, req.stock(), "Alta del producto", username);
        }
        return AdminProductResponse.from(products.saveAndFlush(product));
    }

    @Transactional
    public AdminProductResponse update(long id, ProductRequest req, String username) {
        Product product = find(id);
        requireVersion(product, req.version());
        if (products.existsBySlugAndIdNot(req.slug(), id)) {
            throw new ConflictException("Ya existe un producto con el slug '" + req.slug() + "'.");
        }
        int currentStock = product.getStock();
        product.apply(req.slug(), req.name(), req.category(), req.type(), req.gender(), req.description(), req.price(),
                req.priceMax(), req.wholesalePrice(), req.wholesalePriceMax(), req.imageUrl(), currentStock);
        if (req.stock() != currentStock) {
            stock.record(product, MovementType.AJUSTE, req.stock() - currentStock, "Edición del producto", username);
        }
        return AdminProductResponse.from(products.saveAndFlush(product));
    }

    /**
     * Physical count: sets the stock to what's on the shelf and records the difference as an AJUSTE.
     * {@code expectedVersion} is the version the admin saw; if a sale changed the stock in between,
     * the count is rejected instead of silently erasing that sale.
     */
    @Transactional
    public AdminProductResponse updateStock(long id, int newStock, long expectedVersion, String note, String username) {
        Product product = find(id);
        requireVersion(product, expectedVersion);
        int delta = newStock - product.getStock();
        if (delta != 0) {
            stock.record(product, MovementType.AJUSTE, delta, note == null || note.isBlank() ? "Conteo físico" : note,
                    username);
        }
        return AdminProductResponse.from(products.saveAndFlush(product));
    }

    @Transactional
    public void delete(long id) {
        products.delete(find(id));
    }

    private static void requireVersion(Product product, Long expectedVersion) {
        if (expectedVersion == null || product.getVersion() != expectedVersion) {
            throw new ConflictException("El producto cambió mientras lo editabas. Recarga la lista.");
        }
    }

    private Product find(long id) {
        return products.findById(id).orElseThrow(() -> new NotFoundException("No existe el producto " + id + "."));
    }
}
