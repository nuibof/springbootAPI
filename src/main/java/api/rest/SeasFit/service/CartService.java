package api.rest.SeasFit.service;

import api.rest.SeasFit.entity.Cart;
import api.rest.SeasFit.repository.CartRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class CartService {

    private final CartRepository cartRepository;

    public List<Cart> findAll() {
        return cartRepository.findAll();
    }

    public Optional<Cart> findById(Long id) {
        return cartRepository.findById(id);
    }

    public Cart save(Cart entity) {
        return cartRepository.save(entity);
    }

    public void deleteById(Long id) {
        cartRepository.deleteById(id);
    }
}
