package api.rest.SeasFit.service;

import api.rest.SeasFit.dto.OrderAdminDTO;
import api.rest.SeasFit.dto.OrderItemDTO;
import api.rest.SeasFit.dto.OrderItemRequest;
import api.rest.SeasFit.dto.OrderRequest;
import api.rest.SeasFit.entity.*;
import api.rest.SeasFit.repository.*;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Service
public class OrderService {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepo;
    private final ProductVariantRepository productVariantRepo;
    private final VoucherRepository voucherRepository;
    private final AddressRepository addressRepository;
    private final OrderAddressRepository orderAddressRepository;
    private final RevenueRepository revenueRepository;


    @Transactional
    public Order createOrder(Long userId, OrderRequest request) {
        Address addr = addressRepository.findByIdAndUserId(request.getAddressId(), userId)
                .orElseThrow(() -> new RuntimeException("Địa chỉ không hợp lệ hoặc không thuộc người dùng"));

        BigDecimal total = request.getItems().stream()
                .map(i -> BigDecimal.valueOf(i.getPrice()).multiply(BigDecimal.valueOf(i.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal discount = BigDecimal.ZERO;
        Voucher voucher = null;

        // --- Xử lý voucher (nếu có) ---
        if (request.getVoucherCode() != null && !request.getVoucherCode().isBlank()) {
            voucher = voucherRepository.findByCode(request.getVoucherCode())
                    .orElseThrow(() -> new RuntimeException("Mã giảm giá không tồn tại"));

            var today = LocalDate.now();
            if (voucher.getStartDate() != null && voucher.getStartDate().isAfter(today))
                throw new RuntimeException("Mã giảm giá chưa bắt đầu");
            if (voucher.getEndDate() != null && voucher.getEndDate().isBefore(today))
                throw new RuntimeException("Mã giảm giá đã hết hạn");
            if (!Boolean.TRUE.equals(voucher.getIsActive()))
                throw new RuntimeException("Mã giảm giá không khả dụng");
            if (voucher.getMinOrderAmount() != null && total.compareTo(voucher.getMinOrderAmount()) < 0)
                throw new RuntimeException("Đơn chưa đạt mức tối thiểu áp dụng mã");

            // ❗Trừ lượt ATOMIC (không hoàn kể cả hủy đơn)
            int affected = voucherRepository.decrementQty(voucher.getCode());
            if (affected == 0) {
                throw new RuntimeException("Mã giảm giá đã hết lượt hoặc không khả dụng");
            }

            // Tính discount
            if ("fixed".equalsIgnoreCase(voucher.getDiscountType())) {
                discount = voucher.getDiscountValue();
            } else if ("percent".equalsIgnoreCase(voucher.getDiscountType())) {
                discount = total.multiply(
                        voucher.getDiscountValue().divide(BigDecimal.valueOf(100))
                );
            }
        }

        // --- Tạo order ---
        Order order = new Order();
        order.setUserId(userId);
        order.setAddressId(addr.getId());
        order.setPaymentMethod(request.getPaymentMethod());
        order.setStatus("PENDING");
        order.setNote(request.getNote());
        order.setShippingFee(request.getShippingFee());
        order.setVoucher(voucher);
        order.setVoucherCode(voucher != null ? voucher.getCode() : null);
        order.setTotalAmount(total.intValue());     // tổng trước giảm
        order.setDiscountAmount(discount);          // số tiền giảm
        order.setCreatedAt(LocalDateTime.now());
        order = orderRepository.save(order);

        // Snapshot địa chỉ
        orderAddressRepository.save(OrderAddress.builder()
                .order(order)
                .fullName(addr.getFullName())
                .phone(addr.getPhone())
                .street(addr.getStreet())
                .ward(addr.getWard())
                .district(addr.getDistrict())
                .city(addr.getCity())
                .country(addr.getCountry())
                .build());

        // Items + trừ tồn (đã khóa biến thể)
        for (OrderItemRequest item : request.getItems()) {
            ProductVariant v = productVariantRepo.findForUpdate(
                    item.getProductId(), item.getColorId(), item.getSizeId()
            ).orElseThrow(() -> new RuntimeException("Không tìm thấy biến thể"));

            int qty = item.getQuantity();
            if (v.getQuantity() < qty) throw new RuntimeException("Hết hàng hoặc không đủ tồn kho");

            v.setQuantity(v.getQuantity() - qty);
            productVariantRepo.save(v);

            OrderItem oi = new OrderItem();
            oi.setOrder(order);
            oi.setProductVariant(v);
            oi.setQuantity(qty);
            oi.setPrice(BigDecimal.valueOf(item.getPrice()));
            orderItemRepo.save(oi);
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
        order.setUpdatedAt(LocalDateTime.now());

        orderRepository.save(order);
    }

    public List<Map<String, Object>> getAllOrdersByUserId(Long userId) {
        List<Order> orders = orderRepository.findAllWithItemsByUserId(userId);
        List<Map<String, Object>> result = new ArrayList<>();

        for (Order order : orders) {
            Map<String, Object> orderData = new HashMap<>();
            orderData.put("id",          order.getId());
            orderData.put("createdAt",   order.getCreatedAt());
            orderData.put("status",      order.getStatus());
            orderData.put("paymentMethod", order.getPaymentMethod());
            orderData.put("totalAmount", order.getTotalAmount());
            orderData.put("shippingFee", order.getShippingFee());
            orderData.put("note",        order.getNote()); // nếu FE cần

            // ✅ Lấy trực tiếp từ trường đã lưu
            orderData.put("voucherCode", order.getVoucherCode());
            orderData.put(
                    "discountAmount",
                    order.getDiscountAmount() != null ? order.getDiscountAmount().intValue() : 0
            );

            // Items
            List<Map<String, Object>> items = order.getItems().stream().map(oi -> {
                ProductVariant pv = oi.getProductVariant();
                Map<String, Object> i = new HashMap<>();
                i.put("productId",    pv.getProduct().getId());
                i.put("productName",  pv.getProduct().getName());
                i.put("colorName",    pv.getColor() != null ? pv.getColor().getName() : null);
                i.put("size",         pv.getSize()  != null ? pv.getSize().getLabel() : null);
                i.put("quantity",     oi.getQuantity());
                i.put("price",        oi.getPrice());
                return i;
            }).toList();
            orderData.put("items", items);

            // Địa chỉ snapshot
            orderAddressRepository.findByOrderId(order.getId()).ifPresent(oa -> {
                Map<String, Object> addr = new HashMap<>();
                addr.put("fullName", oa.getFullName());
                addr.put("phone",    oa.getPhone());
                addr.put("street",   oa.getStreet());
                addr.put("ward",     oa.getWard());
                addr.put("district", oa.getDistrict());
                addr.put("city",     oa.getCity());
                addr.put("country",  oa.getCountry());
                orderData.put("address", addr);
            });

            result.add(orderData);
        }
        return result;
    }




    public List<OrderAdminDTO> getAllOrdersWithItems() {
        List<Order> orders = orderRepository.findAll();

        return orders.stream().map(order -> {
            List<OrderItemDTO> itemDTOs = order.getItems().stream().map(item -> {
                ProductVariant pv = null;
                try {
                    pv = item.getProductVariant(); // có thể null
                } catch (jakarta.persistence.EntityNotFoundException ex) {
                    pv = null; // proxy orphan => coi như null
                }

                String productName = "[biến thể đã xoá]";
                String colorName   = "-";
                String sizeLabel   = "-";

                if (pv != null) {
                    try {
                        if (pv.getProduct() != null && pv.getProduct().getName() != null) {
                            productName = pv.getProduct().getName();
                        }
                        if (pv.getColor() != null && pv.getColor().getName() != null) {
                            colorName = pv.getColor().getName();
                        }
                        if (pv.getSize() != null && pv.getSize().getLabel() != null) {
                            sizeLabel = pv.getSize().getLabel();
                        }
                    } catch (jakarta.persistence.EntityNotFoundException ignore) {
                        // giữ fallback
                    }
                }

                return new OrderItemDTO(
                        productName,
                        colorName,
                        sizeLabel,
                        item.getQuantity(),
                        item.getPrice()
                );
            }).collect(Collectors.toList());

            return new OrderAdminDTO(
                    order.getId(),
                    order.getStatus(),
                    order.getPaymentMethod(),
                    order.getVoucher() != null ? order.getVoucher().getCode() : null,
                    order.getNote(),
                    order.getTotalAmount() != null ? BigDecimal.valueOf(order.getTotalAmount()) : BigDecimal.ZERO,
                    order.getDiscountAmount(),
                    order.getShippingFee(),
                    order.getCancelReason(),
                    order.getCreatedAt(),
                    itemDTOs
            );
        }).collect(Collectors.toList());
    }


    @Transactional
    public void updateOrderStatus(Long orderId, String newStatus) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn hàng"));

        String oldStatus = order.getStatus();
        if (Objects.equals(oldStatus, newStatus)) return;

        // Hoàn tồn kho khi chuyển sang CANCELLED
        if (!"CANCELLED".equals(oldStatus) && "CANCELLED".equals(newStatus)) {
            for (OrderItem oi : orderItemRepo.findByOrderId(orderId)) {
                ProductVariant v = productVariantRepo.findForUpdate(
                        oi.getProductVariant().getProduct().getId(),
                        oi.getProductVariant().getColor().getId().longValue(),
                        oi.getProductVariant().getSize().getId().longValue()
                ).orElseThrow();

                v.setQuantity(v.getQuantity() + oi.getQuantity()); // hoàn tồn
                productVariantRepo.save(v);
            }
            // Xóa doanh thu nếu đã ghi trước đó
            revenueRepository.deleteByOrderId(orderId);
        }

        // Ghi doanh thu khi đơn chuyển sang DELIVERED từ trạng thái khác
        if (!"DELIVERED".equals(oldStatus) && "DELIVERED".equals(newStatus)) {
            Revenue revenue = new Revenue();
            revenue.setOrder(order);
            revenue.setAmount(BigDecimal.valueOf(order.getTotalAmount() != null ? order.getTotalAmount() : 0));
            revenue.setCreatedAt(LocalDateTime.now());
            revenueRepository.save(revenue);
        }

        order.setStatus(newStatus);
        orderRepository.save(order);
    }





}

