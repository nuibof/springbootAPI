package api.rest.SeasFit.repository;

import api.rest.SeasFit.entity.Cart;
import api.rest.SeasFit.entity.CartItem;
import api.rest.SeasFit.entity.ProductVariant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

import java.util.Optional;

public interface CartItemRepository extends JpaRepository<CartItem, Long> {
    Optional<CartItem> findByCartAndVariant(Cart cart, ProductVariant variant);
    List<CartItem> findByCart(Cart cart);

    Optional<CartItem> findByIdAndCart_User_Id(Long id, Long userId);


    @Modifying
    @Query("DELETE FROM CartItem ci WHERE ci.variant.product.id = :productId")
    void deleteByProductId(@Param("productId") Long productId);

    Optional<CartItem> findByCart_User_IdAndVariant_Id(Long userId, Long variantId);

    @Modifying
    @Query("DELETE FROM CartItem ci WHERE ci.cart.user.id = :userId AND ci.id IN :itemIds")
    void deleteByUserIdAndItemIds(@Param("userId") Long userId, @Param("itemIds") List<Long> itemIds);

    void deleteByIdInAndCartUserId(List<Long> ids, Long userId);
}
