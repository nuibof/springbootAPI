package api.rest.SeasFit.service;

import api.rest.SeasFit.dto.OrderItemRequest;
import api.rest.SeasFit.dto.OrderRequest;
import api.rest.SeasFit.entity.Order;
import api.rest.SeasFit.entity.OrderItem;
import api.rest.SeasFit.entity.ProductVariant;
import api.rest.SeasFit.repository.OrderItemRepository;
import api.rest.SeasFit.repository.OrderRepository;
import api.rest.SeasFit.repository.ProductVariantRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@RequiredArgsConstructor
@Service
public class OrderService {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepo;
    private final ProductVariantRepository productVariantRepo;

    public Order createOrder(Long userId, OrderRequest request) {
        long total = request.getItems().stream()
                .mapToLong(item -> item.getPrice() * item.getQuantity())
                .sum();

        Order order = new Order();
        order.setUserId(userId);
        order.setAddressId(request.getAddressId());
        order.setPaymentMethod(request.getPaymentMethod());
        order.setStatus("PENDING");
        order.setTotalAmount((int) total);
        order.setCreatedAt(LocalDateTime.now());

        order = orderRepository.save(order);

        for (OrderItemRequest item : request.getItems()) {
            // ✅ Tìm variantId từ productId + colorId + sizeId
            ProductVariant variant = productVariantRepo.findByProductIdAndColorIdAndSizeId(
                    item.getProductId(),
                    item.getColorId(),
                    item.getSizeId()
            ).orElseThrow(() -> new RuntimeException("Không tìm thấy biến thể sản phẩm"));

            OrderItem orderItem = new OrderItem();
            orderItem.setOrderId(order.getId());
            orderItem.setVariantId(variant.getId());
            orderItem.setQuantity(item.getQuantity());
            orderItem.setPrice(BigDecimal.valueOf(item.getPrice()));

            orderItemRepo.save(orderItem);
        }

        return order;
    }

    public int getTotalOrders() {
        return (int) orderRepository.count();
    }

    @Transactional
    public void markAsPaid(String orderId, String transId) {
        Long id;
        try {
            id = Long.parseLong(orderId);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Order ID không hợp lệ: " + orderId);
        }

        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn hàng: " + orderId));

        // Nếu đã thanh toán rồi thì không cập nhật nữa (MoMo có thể gửi lại IPN nhiều lần)
        if ("PAID".equalsIgnoreCase(order.getStatus())) {
            return;
        }

        order.setStatus("PAID");
        order.setPaymentTransactionId(transId);
        order.setUpdatedAt(LocalDateTime.now());

        orderRepository.save(order);
    }

}

