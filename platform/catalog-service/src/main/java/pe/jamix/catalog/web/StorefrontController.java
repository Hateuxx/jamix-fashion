package pe.jamix.catalog.web;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import pe.jamix.catalog.config.JamixProperties;
import pe.jamix.catalog.product.Category;
import pe.jamix.catalog.product.ProductResponse;
import pe.jamix.catalog.product.ProductService;
import tools.jackson.databind.json.JsonMapper;

/**
 * Server-rendered storefront: hero, indexable catalogue, wholesale banner and the lead form.
 * Filtering works through plain links (?categoria=ella) so crawlers and no-JS visitors get it too.
 */
@Controller
public class StorefrontController {

    private final ProductService products;
    private final JamixProperties.Storefront storefront;
    private final JsonMapper jsonMapper;
    private final PhotoSources photos;

    public StorefrontController(ProductService products, JamixProperties properties, JsonMapper jsonMapper,
            PhotoSources photos) {
        this.products = products;
        this.storefront = properties.storefront();
        this.jsonMapper = jsonMapper;
        this.photos = photos;
    }

    @GetMapping("/")
    public String home(@RequestParam(name = "categoria", required = false) String categoria,
            @RequestParam(name = "lead", required = false) String lead,
            Model model) {
        Optional<Category> active = Category.fromSlug(categoria);
        List<ProductResponse> catalog = products.catalog(active);

        model.addAttribute("products", catalog);
        model.addAttribute("photos", photos);
        model.addAttribute("categories", Category.values());
        model.addAttribute("activeCategory", active.map(c -> c.slug()).orElse(null));
        model.addAttribute("whatsappNumber", storefront.whatsappNumber());
        model.addAttribute("leadsEndpoint", storefront.leadsEndpoint());
        model.addAttribute("shopUrl", storefront.shopUrl());
        model.addAttribute("canonicalUrl", storefront.siteUrl() + active.map(c -> "/?categoria=" + c.slug()).orElse("/"));
        model.addAttribute("leadStatus", "error".equals(lead) ? "error" : null);
        model.addAttribute("jsonLd", jsonLd(catalog));
        return "index";
    }

    /** schema.org ClothingStore + offers, escaped so a product name can never close the script tag. */
    private String jsonLd(List<ProductResponse> catalog) {
        Map<String, Object> store = new LinkedHashMap<>();
        store.put("@context", "https://schema.org");
        store.put("@type", "ClothingStore");
        store.put("name", "Jamix Fashion");
        store.put("url", storefront.siteUrl());
        store.put("telephone", "+" + storefront.whatsappNumber());
        store.put("address", Map.of("@type", "PostalAddress", "addressLocality", "San Juan de Lurigancho, Lima",
                "addressCountry", "PE"));
        store.put("makesOffer", catalog.stream().map(p -> {
            Map<String, Object> offer = new LinkedHashMap<>();
            offer.put("@type", "Offer");
            offer.put("price", p.price());
            offer.put("priceCurrency", "PEN");
            offer.put("availability", p.inStock() ? "https://schema.org/InStock" : "https://schema.org/OutOfStock");
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("@type", "Product");
            item.put("name", p.name());
            if (p.description() != null) {
                item.put("description", p.description());
            }
            if (p.imageUrl() != null) {
                item.put("image", storefront.siteUrl() + p.imageUrl());
            }
            offer.put("itemOffered", item);
            return offer;
        }).toList());
        return jsonMapper.writeValueAsString(store).replace("<", "\\u003c");
    }
}
