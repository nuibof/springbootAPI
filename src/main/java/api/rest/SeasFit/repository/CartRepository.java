package api.rest.SeasFit.repository;

import api.rest.SeasFit.entity.Cart;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CartRepository extends JpaRepository<Cart, Long> {
}
