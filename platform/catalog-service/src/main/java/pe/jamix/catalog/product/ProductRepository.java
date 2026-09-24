package pe.jamix.catalog.product;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductRepository extends JpaRepository<Product, Long> {

    // The gallery is fetched in the same query, instead of one extra query per product.
    @EntityGraph(attributePaths = "gallery")
    List<Product> findAllByOrderByIdAsc();

    @EntityGraph(attributePaths = "gallery")
    List<Product> findByCategoryOrderByIdAsc(Category category);

    @EntityGraph(attributePaths = "gallery")
    Optional<Product> findBySlug(String slug);

    boolean existsBySlug(String slug);

    boolean existsBySlugAndIdNot(String slug, Long id);
}
