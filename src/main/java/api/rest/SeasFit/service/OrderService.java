package api.rest.SeasFit.service;

import api.rest.SeasFit.entity.Order;
import api.rest.SeasFit.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;

    public List<Order> findAll() {
        return orderRepository.findAll();
    }

    public Optional<Order> findById(Long id) {
        return orderRepository.findById(id);
    }

    public Order save(Order entity) {
        return orderRepository.save(entity);
    }

    public void deleteById(Long id) {
        orderRepository.deleteById(id);
    }

    public int getTotalOrders() {
        return (int) orderRepository.count();
    }
}
