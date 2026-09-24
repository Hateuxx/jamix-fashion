package pe.jamix.catalog.order;

import java.util.List;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderRepository extends JpaRepository<CustomerOrder, Long> {

    @EntityGraph(attributePaths = "lines")
    List<CustomerOrder> findByCustomerUsernameOrderByCreatedAtDesc(String customerUsername);
}
