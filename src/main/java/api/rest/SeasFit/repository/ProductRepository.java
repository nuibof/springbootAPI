package api.rest.SeasFit.repository;

import api.rest.SeasFit.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface ProductRepository extends JpaRepository<Product, Long> {
    @Query(value = """
    SELECT COUNT(*)
    FROM product p
    WHERE (
        SELECT SUM(pv.quantity)
        FROM product_variant pv
        WHERE pv.product_id = p.id
    ) < 10
""", nativeQuery = true)
    int countProductsLowStock();
}
