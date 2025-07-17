package api.rest.SeasFit.service;

import api.rest.SeasFit.entity.CartItem;
import api.rest.SeasFit.repository.CartItemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class CartItemService {

    private final CartItemRepository cartItemRepository;

    public List<CartItem> findAll() {
        return cartItemRepository.findAll();
    }

    public Optional<CartItem> findById(Long id) {
        return cartItemRepository.findById(id);
    }

    public CartItem save(CartItem entity) {
        return cartItemRepository.save(entity);
    }

    public void deleteById(Long id) {
        cartItemRepository.deleteById(id);
    }
}
