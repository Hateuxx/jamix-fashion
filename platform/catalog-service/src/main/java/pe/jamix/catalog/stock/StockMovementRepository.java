package pe.jamix.catalog.stock;

import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StockMovementRepository extends JpaRepository<StockMovement, Long> {

    List<StockMovement> findByProductIdOrderByCreatedAtDescIdDesc(Long productId, Pageable page);

    List<StockMovement> findAllByOrderByCreatedAtDescIdDesc(Pageable page);
}
