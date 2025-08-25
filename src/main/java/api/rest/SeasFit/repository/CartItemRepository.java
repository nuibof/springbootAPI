package api.rest.SeasFit.repository;

import api.rest.SeasFit.entity.Cart;
import api.rest.SeasFit.entity.CartItem;
import api.rest.SeasFit.entity.ProductVariant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;

import java.util.List;
import java.util.Optional;

public interface CartItemRepository extends JpaRepository<CartItem, Long> {

    Optional<CartItem> findByCartAndVariant(Cart cart, ProductVariant variant);
    List<CartItem> findByCart(Cart cart);

    Optional<CartItem> findByIdAndCart_User_Id(Long id, Long userId);
    Optional<CartItem> findByCart_User_IdAndVariant_Id(Long userId, Long variantId);

    // Xoá theo PRODUCT (nếu còn dùng)
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    int deleteByVariant_Product_Id(Long productId);

    // ❌ đừng dùng: tên sai
    // void deleteByIdInAndCartUserId(...);

    // ✅ đúng path: cart.user.id + id in (...)
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    int deleteByCart_User_IdAndIdIn(Long userId, List<Long> ids);

    // ✅ đúng path: cart.user.id + variant.id in (...)
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    int deleteByCart_User_IdAndVariant_IdIn(Long userId, List<Long> variantIds);
}
