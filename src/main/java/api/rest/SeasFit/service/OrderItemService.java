package api.rest.SeasFit.service;

import api.rest.SeasFit.entity.OrderItem;
import api.rest.SeasFit.repository.OrderItemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class OrderItemService {

    private final OrderItemRepository orderItemRepository;

    public List<OrderItem> findAll() {
        return orderItemRepository.findAll();
    }

    public Optional<OrderItem> findById(Long id) {
        return orderItemRepository.findById(id);
    }

    public OrderItem save(OrderItem entity) {
        return orderItemRepository.save(entity);
    }

    public void deleteById(Long id) {
        orderItemRepository.deleteById(id);
    }
}
