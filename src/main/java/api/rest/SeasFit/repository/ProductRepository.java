package api.rest.SeasFit.repository;

import api.rest.SeasFit.dto.FavoriteAdminDTO;
import api.rest.SeasFit.entity.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;

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

    @Query(value = """
        SELECT *
        FROM product
        WHERE gender = ?
            AND
                status = 'ACTIVE'
    """, nativeQuery = true)
    List<Product> findByGender(int gender);

    Page<Product> findAll(Specification<Product> spec, Pageable pageable);

    @Modifying
    @Query(value = "UPDATE product SET status = 'DELETE' WHERE id = :id", nativeQuery = true)
    int softDeleteById(@Param("id") Long id);

    @EntityGraph(attributePaths = {"variants","variants.color","variants.size"})
    Page<Product> findAllByStatus(String status, Specification<Product> spec, Pageable pageable);

    @Query("SELECT DISTINCT p FROM Product p " +
            "JOIN p.variants v " +
            "JOIN v.color c " +
            "JOIN v.size s " +
            "WHERE (:categoryId IS NULL OR p.category.id = :categoryId) " +
            "AND (:colorId IS NULL OR c.id = :colorId) " +
            "AND (:sizeId IS NULL OR s.id = :sizeId) " +
            "AND (:minPrice IS NULL OR v.price >= :minPrice) " +
            "AND (:maxPrice IS NULL OR v.price <= :maxPrice)")
    List<Product> findAllWithFilters(
            @Param("categoryId") Long categoryId,
            @Param("colorId") Integer colorId,
            @Param("sizeId") Integer sizeId,
            @Param("minPrice") BigDecimal minPrice,
            @Param("maxPrice") BigDecimal maxPrice);


    @Query("""
    SELECT p FROM Product p
    WHERE (:keyword IS NULL OR LOWER(p.name) LIKE LOWER(CONCAT('%', :keyword, '%')))
      AND (:status IS NULL OR p.status = :status)
      AND (:categoryId IS NULL OR p.category.id = :categoryId)
      AND (:gender IS NULL OR p.gender = :gender)
""")
    Page<Product> searchAdminProducts(
            @Param("keyword") String keyword,
            @Param("status") String status,
            @Param("categoryId") Long categoryId,
            @Param("gender") Integer gender,
            Pageable pageable
    );

    @Query("""
    select p from Product p
    where lower(p.name) like lower(concat('%', :q, '%')) AND status like 'ACTIVE'
    order by p.createdAt desc, p.id desc
""")
    List<Product> searchTop(@Param("q") String q, Pageable pageable);

    Page<Product> findByNameContainingIgnoreCase(String q, Pageable pageable);

    @Query(value = """
    select new api.rest.SeasFit.dto.FavoriteAdminDTO(
      p.id, p.name, p.imageUrl, count(f.id)
    )
    from Product p
    left join Favorite f on f.productId = p.id
    where (:q is null or :q = '' or lower(p.name) like lower(concat('%', :q, '%')))
    group by p.id, p.name, p.imageUrl
    order by count(f.id) desc, p.id asc
    """,
            countQuery = """
    select count(p.id)
    from Product p
    where (:q is null or :q = '' or lower(p.name) like lower(concat('%', :q, '%')))
    """)
    Page<FavoriteAdminDTO> pageFavoriteStats(@Param("q") String q, Pageable pageable);

    
}
