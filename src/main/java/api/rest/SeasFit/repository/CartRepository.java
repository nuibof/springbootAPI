package api.rest.SeasFit.repository;

import api.rest.SeasFit.entity.Cart;
import api.rest.SeasFit.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CartRepository extends JpaRepository<Cart, Long> {
    Optional<Cart> findByUser(User user);
}
