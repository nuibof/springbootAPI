package api.rest.SeasFit.service;

import api.rest.SeasFit.dto.*;
import api.rest.SeasFit.entity.*;
import api.rest.SeasFit.repository.*;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Predicate;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
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
    private final CartItemRepository cartItemRepository;

    @Transactional
    public Order createOrder(Long userId, OrderRequest request) {
        // 1) Validate địa chỉ
        Address addr = addressRepository.findByIdAndUserId(request.getAddressId(), userId)
                .orElseThrow(() -> new RuntimeException("Địa chỉ không hợp lệ hoặc không thuộc người dùng"));

        // 2) Duyệt items: lock biến thể, check tồn, tính subtotal theo GIÁ SAU SALE (server-side)
        BigDecimal subtotal = BigDecimal.ZERO;
        List<Long> orderedVariantIds = new ArrayList<>();
        // cache để khỏi gọi DB 2 lần
        record Row(ProductVariant variant, int qty, BigDecimal unitBase, BigDecimal unitFinal) {}
        List<Row> rows = new ArrayList<>();

        for (OrderItemRequest item : request.getItems()) {
            ProductVariant v = productVariantRepo.findForUpdate(
                    item.getProductId(), item.getColorId(), item.getSizeId()
            ).orElseThrow(() -> new RuntimeException("Không tìm thấy biến thể"));

            int qty = item.getQuantity();
            if (qty <= 0) throw new RuntimeException("Số lượng không hợp lệ");
            if (v.getQuantity() < qty) throw new RuntimeException("Hết hàng hoặc không đủ tồn kho");

            BigDecimal unitBase  = Optional.ofNullable(v.getPrice()).orElse(BigDecimal.ZERO);
            BigDecimal unitFinal = Optional.ofNullable(v.getEffectivePrice()).orElse(unitBase);

            subtotal = subtotal.add(unitFinal.multiply(BigDecimal.valueOf(qty)));
            rows.add(new Row(v, qty, unitBase, unitFinal));
            orderedVariantIds.add(v.getId());
        }

        // 3) Voucher (tính trên subtotal sau sale)
        BigDecimal discount = BigDecimal.ZERO;
        Voucher voucher = null;

        if (request.getVoucherCode() != null && !request.getVoucherCode().isBlank()) {
            voucher = voucherRepository.findByCode(request.getVoucherCode())
                    .orElseThrow(() -> new RuntimeException("Mã giảm giá không tồn tại"));

            LocalDate today = LocalDate.now();
            if (voucher.getStartDate() != null && voucher.getStartDate().isAfter(today))
                throw new RuntimeException("Mã giảm giá chưa bắt đầu");
            if (voucher.getEndDate() != null && voucher.getEndDate().isBefore(today))
                throw new RuntimeException("Mã giảm giá đã hết hạn");
            if (!Boolean.TRUE.equals(voucher.getIsActive()))
                throw new RuntimeException("Mã giảm giá không khả dụng");

            if (voucher.getMinOrderAmount() != null && subtotal.compareTo(voucher.getMinOrderAmount()) < 0)
                throw new RuntimeException("Đơn chưa đạt mức tối thiểu áp dụng mã");

            // Decrement atomic (không hoàn dù huỷ đơn)
//            int affected = voucherRepository.decrementQty(voucher.getCode());
//            if (affected == 0) throw new RuntimeException("Mã giảm giá đã hết lượt hoặc không khả dụng");

            // Tính giảm
            if ("fixed".equalsIgnoreCase(voucher.getDiscountType())) {
                discount = Optional.ofNullable(voucher.getDiscountValue()).orElse(BigDecimal.ZERO);
            } else if ("percent".equalsIgnoreCase(voucher.getDiscountType())) {
                BigDecimal pct = Optional.ofNullable(voucher.getDiscountValue()).orElse(BigDecimal.ZERO);
                discount = subtotal.multiply(pct.divide(BigDecimal.valueOf(100)));
            }

            // Giới hạn max giảm (nếu có)
            if (voucher.getMaxDiscountValue() != null && discount.compareTo(voucher.getMaxDiscountValue()) > 0) {
                discount = voucher.getMaxDiscountValue();
            }

            if (discount.signum() < 0) discount = BigDecimal.ZERO;
        }

        // 4) Tạo order (totalAmount = subtotal SAU SALE, TRƯỚC voucher)
        Order order = new Order();
        order.setUserId(userId);
        order.setAddressId(addr.getId());
        order.setPaymentMethod(request.getPaymentMethod());
        order.setStatus("PENDING");
        order.setNote(request.getNote());
        order.setShippingFee(request.getShippingFee());
        order.setVoucher(voucher);
        order.setVoucherCode(voucher != null ? voucher.getCode() : null);
        order.setTotalAmount(subtotal.intValue());
        order.setDiscountAmount(discount);
        order.setCreatedAt(LocalDateTime.now());
        order = orderRepository.save(order);

        // 5) Snapshot địa chỉ


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

        // 6) Lưu items + trừ tồn (đơn giá = unitFinal sau sale)
        for (Row r : rows) {
            ProductVariant v = r.variant();
            int qty = r.qty();

            v.setQuantity(v.getQuantity() - qty);
            productVariantRepo.save(v);

            OrderItem oi = new OrderItem();
            oi.setOrder(order);
            oi.setProductVariant(v);
            oi.setQuantity(qty);
            oi.setPrice(r.unitFinal());         // ✅ giá đã giảm
            // nếu có cột originalPrice thì có thể lưu r.unitBase()
            orderItemRepo.save(oi);
        }

        // 7) XÓA GIỎ
        List<Long> cartIds = Optional.ofNullable(request.getCartItemIds()).orElseGet(Collections::emptyList);
        if (!cartIds.isEmpty()) {
            cartItemRepository.deleteByCart_User_IdAndIdIn(userId, cartIds);
        } else if (!orderedVariantIds.isEmpty()) {
            cartItemRepository.deleteByCart_User_IdAndVariant_IdIn(userId, orderedVariantIds);
        }

        return order;
    }

    // Trong OrderService
    @Transactional
    public Order updateOrderEditableFields(Long userId, Long orderId, UpdateOrderRequest req) {
        Order order = orderRepository.findByIdAndUserId(orderId, userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Đơn hàng không tồn tại"));

        if (!"PENDING".equals(order.getStatus())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Chỉ được sửa khi đơn còn PENDING");
        }

        // 1) Địa chỉ: validate thuộc về user + cập nhật snapshot
        if (req.getAddressId() != null && !req.getAddressId().equals(order.getAddressId())) {
            Address addr = addressRepository.findByIdAndUserId(req.getAddressId(), userId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Địa chỉ không hợp lệ"));
            order.setAddressId(addr.getId());

            OrderAddress oa = orderAddressRepository.findByOrderId(order.getId())
                    .orElse(OrderAddress.builder().order(order).build());
            oa.setFullName(addr.getFullName());
            oa.setPhone(addr.getPhone());
            oa.setStreet(addr.getStreet());
            oa.setWard(addr.getWard());
            oa.setDistrict(addr.getDistrict());
            oa.setCity(addr.getCity());
            oa.setCountry(addr.getCountry());
            orderAddressRepository.save(oa);
        }

        // 2) paymentMethod
        if (req.getPaymentMethod() != null && !req.getPaymentMethod().isBlank()) {
            String pm = req.getPaymentMethod().trim().toUpperCase();
            if (!Set.of("COD","VNPAY","MOMO","BANKING").contains(pm)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "paymentMethod không hợp lệ");
            }
            order.setPaymentMethod(pm);
        }

        // 3) shippingFee
        if (req.getShippingFee() != null) {
            if (req.getShippingFee().signum() < 0) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "shippingFee phải >= 0");
            }
            order.setShippingFee(req.getShippingFee().intValue());
        }

        // 4) note
        if (req.getNote() != null) {
            order.setNote(req.getNote().trim());
        }

        // 5) voucherCode: cho phép set/chuyển/clear (không decrement ở đây)
        if (req.getVoucherCode() != null) {
            String code = req.getVoucherCode().trim();
            if (code.isEmpty()) {
                order.setVoucher(null);
                order.setVoucherCode(null);
                order.setDiscountAmount(BigDecimal.ZERO);
            } else {
                Voucher v = voucherRepository.findByCode(code)
                        .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Mã giảm giá không tồn tại"));
                BigDecimal subtotal = computeOrderSubtotal(order.getId());
                LocalDate today = LocalDate.now();
                if (v.getStartDate() != null && v.getStartDate().isAfter(today))
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Mã giảm giá chưa bắt đầu");
                if (v.getEndDate() != null && v.getEndDate().isBefore(today))
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Mã giảm giá đã hết hạn");
                if (!Boolean.TRUE.equals(v.getIsActive()))
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Mã giảm giá không khả dụng");
                if (v.getMinOrderAmount() != null && subtotal.compareTo(v.getMinOrderAmount()) < 0)
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Đơn chưa đạt mức tối thiểu áp dụng mã");

                BigDecimal discount = computeDiscount(subtotal, v);
                order.setVoucher(v);
                order.setVoucherCode(v.getCode());
                order.setDiscountAmount(discount);
            }
        }


        return orderRepository.save(order);
    }

    public int getTotalOrders() {
        return (int) orderRepository.count();
    }

    @Transactional
    public void markAsPaid(String orderId, String transId) {
        Long id = Long.parseLong(orderId);
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn hàng: " + orderId));

        if ("PAID".equalsIgnoreCase(order.getStatus())) return;

        order.setStatus("PAID");
        order.setUpdatedAt(LocalDateTime.now());

        // chỉ online mới trừ ở đây
        if (!"COD".equalsIgnoreCase(order.getPaymentMethod())) {
            consumeVoucherIfNeeded(order);
        }
        orderRepository.save(order);
    }


    public List<Map<String, Object>> getAllOrdersByUserId(Long userId) {
        List<OrderRepository.OrderItemFlatUser> rows = orderRepository.findOrdersForUserFlat(userId);

        Map<Long, List<OrderRepository.OrderItemFlatUser>> byOrder = new LinkedHashMap<>();
        for (var r : rows) {
            byOrder.computeIfAbsent(r.getOrderId(), k -> new ArrayList<>()).add(r);
        }

        List<Map<String, Object>> result = new ArrayList<>();
        for (var entry : byOrder.entrySet()) {
            var list = entry.getValue();
            var head = list.get(0);

            List<Map<String, Object>> items = new ArrayList<>();
            for (var r : list) {
                if (r.getQuantity() == null && r.getPrice() == null && r.getProductId() == null && r.getProductName() == null) {
                    continue;
                }
                String productName = r.getProductName() != null ? r.getProductName() : "[biến thể đã xoá]";
                String colorName   = r.getColorName()   != null ? r.getColorName()   : "-";
                String sizeLabel   = r.getSizeLabel()   != null ? r.getSizeLabel()   : "-";

                Map<String, Object> i = new HashMap<>();
                i.put("productId",   r.getProductId());
                i.put("productName", productName);
                i.put("colorName",   colorName);
                i.put("size",        sizeLabel);
                i.put("quantity",    r.getQuantity() != null ? r.getQuantity() : 0);
                i.put("price",       r.getPrice());
                items.add(i);
            }

            Map<String, Object> orderData = new LinkedHashMap<>();
            orderData.put("id",            head.getOrderId());
            orderData.put("createdAt",     head.getCreatedAt());
            orderData.put("status",        head.getStatus());
            orderData.put("paymentMethod", head.getPaymentMethod());
            orderData.put("totalAmount",   head.getTotalAmount());
            orderData.put("shippingFee",   head.getShippingFee());
            orderData.put("note",          head.getNote());
            orderData.put("voucherCode",   head.getVoucherCode());
            orderData.put("discountAmount",head.getDiscountAmount() == null ? 0 : head.getDiscountAmount().intValue());
            orderData.put("items",         items);

            if (head.getAddrFullName() != null || head.getAddrPhone() != null) {
                Map<String, Object> addr = new LinkedHashMap<>();
                addr.put("fullName", head.getAddrFullName());
                addr.put("phone",    head.getAddrPhone());
                addr.put("street",   head.getAddrStreet());
                addr.put("ward",     head.getAddrWard());
                addr.put("district", head.getAddrDistrict());
                addr.put("city",     head.getAddrCity());
                addr.put("country",  head.getAddrCountry());
                orderData.put("address", addr);
            }

            result.add(orderData);
        }

        return result;
    }

    public List<OrderAdminDTO> getAllOrdersWithItems() {
        List<OrderRepository.OrderItemFlat> rows = orderRepository.findAllFlatIncludingSoftDeleted(null);

        Map<Long, List<OrderRepository.OrderItemFlat>> byOrder =
                rows.stream().collect(Collectors.groupingBy(
                        OrderRepository.OrderItemFlat::getOrderId,
                        LinkedHashMap::new,
                        Collectors.toList()
                ));

        List<OrderAdminDTO> out = new ArrayList<>();
        for (var entry : byOrder.entrySet()) {
            var list = entry.getValue();

            List<OrderItemDTO> items = list.stream().map(r -> new OrderItemDTO(
                    r.getProductName() != null ? r.getProductName() : "[biến thể đã xoá]",
                    r.getColorName()   != null ? r.getColorName()   : "-",
                    r.getSizeLabel()   != null ? r.getSizeLabel()   : "-",
                    Optional.ofNullable(r.getQuantity()).orElse(0),
                    Optional.ofNullable(r.getPrice()).orElse(BigDecimal.ZERO)
            )).toList();

            var h = list.get(0);
            out.add(new OrderAdminDTO(
                    h.getOrderId(),
                    h.getStatus(),
                    h.getPaymentMethod(),
                    h.getVoucherCode(),
                    h.getNote(),
                    h.getTotalAmount().intValue(),
                    h.getDiscountAmount(),
                    h.getShippingFee() != null ? h.getShippingFee().intValue() : 0,
                    h.getCancelReason(),
                    h.getCreatedAt() != null
                            ? h.getCreatedAt().atZone(ZoneId.systemDefault()).toLocalDateTime()
                            : null,
                    items
            ));
        }
        return out;
    }

    @Transactional
    public void updateOrderStatus(Long orderId, String newStatus) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn hàng"));

        String oldStatus = Optional.ofNullable(order.getStatus()).orElse("");
        String toStatus  = Optional.ofNullable(newStatus).orElse("").toUpperCase(Locale.ROOT);

        if (Objects.equals(oldStatus, toStatus)) return;

        // ---- HỦY ĐƠN: hoàn tồn, xoá revenue ----
        if (!"CANCELLED".equals(oldStatus) && "CANCELLED".equals(toStatus)) {
            for (OrderItem oi : orderItemRepo.findByOrderId(orderId)) {
                ProductVariant v = productVariantRepo.findForUpdate(
                        oi.getProductVariant().getProduct().getId(),
                        oi.getProductVariant().getColor().getId().longValue(),
                        oi.getProductVariant().getSize().getId().longValue()
                ).orElseThrow();

                v.setQuantity(v.getQuantity() + oi.getQuantity()); // hoàn tồn
                productVariantRepo.save(v);
            }
            revenueRepository.deleteByOrderId(orderId);

            order.setStatus(toStatus);
            order.setUpdatedAt(LocalDateTime.now());
            orderRepository.save(order);
            return;
        }

        // ---- ĐÃ GIAO: trừ voucher (COD) + ghi doanh thu NET ----
        if (!"DELIVERED".equals(oldStatus) && "DELIVERED".equals(toStatus)) {
            if ("COD".equalsIgnoreCase(order.getPaymentMethod())) {
                consumeVoucherIfNeeded(order);
            }
            if ("MOMO".equalsIgnoreCase(order.getPaymentMethod())) {
                consumeVoucherIfNeeded(order);
            }

            order.setStatus(toStatus);
            order.setUpdatedAt(LocalDateTime.now());
            orderRepository.save(order);

            Revenue revenue = new Revenue();
            revenue.setOrder(order);
            revenue.setAmount(calcNetAmount(order));
            revenue.setCreatedAt(LocalDateTime.now());
            revenueRepository.save(revenue);
            return;
        }


        // ---- Các chuyển trạng thái khác ----
        order.setStatus(toStatus);
        order.setUpdatedAt(LocalDateTime.now());
        orderRepository.save(order);
    }

    /** Trừ lượt voucher khi thật sự hoàn tất (online: ở markAsPaid; COD: ở DELIVERED).
     *  Nếu giảm thất bại (hết lượt), fallback: bỏ voucher & discount = 0.
     */
    private void consumeVoucherIfNeeded(Order order) {
        String code = Optional.ofNullable(order.getVoucherCode()).orElse("").trim();
        if (code.isEmpty()) return;
        if (order.getDiscountAmount() == null || order.getDiscountAmount().signum() <= 0) return;

        int affected = voucherRepository.decrementQty(code);
        if (affected == 0) {
            // không throw, không sửa order; chỉ log nếu cần
        }
    }


    /** Doanh thu thuần: subtotal(sau sale) - discount + shippingFee, không âm. */
    private BigDecimal calcNetAmount(Order o) {
        BigDecimal subtotal = BigDecimal.valueOf(Optional.ofNullable(o.getTotalAmount()).orElse(0));
        BigDecimal discount = Optional.ofNullable(o.getDiscountAmount()).orElse(BigDecimal.ZERO);
        BigDecimal ship     = BigDecimal.valueOf(Optional.ofNullable(o.getShippingFee()).orElse(0));
        BigDecimal net = subtotal.subtract(discount).add(ship);
        return net.signum() < 0 ? BigDecimal.ZERO : net;
    }


    public List<OrderAdminDTO> getAllOrdersWithItemsFiltered(
            String q, String status, String payment, LocalDate from, LocalDate to,
            Long min, Long max, String sort
    ) {
        List<OrderRepository.OrderItemFlat> rows = orderRepository.findAllFlatIncludingSoftDeleted(null);

        Map<Long, List<OrderRepository.OrderItemFlat>> byOrder =
                rows.stream().collect(Collectors.groupingBy(
                        OrderRepository.OrderItemFlat::getOrderId,
                        LinkedHashMap::new,
                        Collectors.toList()
                ));

        List<OrderAdminDTO> list = new ArrayList<>();
        for (var entry : byOrder.entrySet()) {
            var listFlat = entry.getValue();
            var h = listFlat.get(0);

            List<OrderItemDTO> items = listFlat.stream().map(r -> new OrderItemDTO(
                    r.getProductName() != null ? r.getProductName() : "[biến thể đã xoá]",
                    r.getColorName()   != null ? r.getColorName()   : "-",
                    r.getSizeLabel()   != null ? r.getSizeLabel()   : "-",
                    Optional.ofNullable(r.getQuantity()).orElse(0),
                    Optional.ofNullable(r.getPrice()).orElse(BigDecimal.ZERO)
            )).toList();

            list.add(new OrderAdminDTO(
                    h.getOrderId(),
                    h.getStatus(),
                    h.getPaymentMethod(),
                    h.getVoucherCode(),
                    h.getNote(),
                    h.getTotalAmount() != null ? h.getTotalAmount().intValue() : 0,
                    h.getDiscountAmount(),
                    h.getShippingFee() != null ? h.getShippingFee().intValue() : 0,
                    h.getCancelReason(),
                    h.getCreatedAt() != null
                            ? h.getCreatedAt().atZone(ZoneId.systemDefault()).toLocalDateTime()
                            : null,
                    items
            ));
        }

        String qx = q == null ? "" : q.trim().toLowerCase(Locale.ROOT);
        LocalDateTime fromDt = (from != null) ? from.atStartOfDay() : null;
        LocalDateTime toDt   = (to   != null) ? to.atTime(23,59,59, 999_000_000) : null;

        list = list.stream().filter(o -> {
            if (!qx.isEmpty()) {
                boolean hit = String.valueOf(o.getId()).contains(qx)
                        || (o.getStatus() != null && o.getStatus().toLowerCase(Locale.ROOT).contains(qx))
                        || (o.getVoucherCode() != null && o.getVoucherCode().toLowerCase(Locale.ROOT).contains(qx))
                        || (o.getNote() != null && o.getNote().toLowerCase(Locale.ROOT).contains(qx));
                if (!hit) return false;
            }
            if (status != null && !status.isBlank() && !status.equals(o.getStatus())) return false;
            if (payment != null && !payment.isBlank() && !payment.equals(o.getPaymentMethod())) return false;

            if (fromDt != null && (o.getCreatedAt() == null || o.getCreatedAt().isBefore(fromDt))) return false;
            if (toDt   != null && (o.getCreatedAt() == null || o.getCreatedAt().isAfter(toDt)))     return false;

            long total = safeLong(o.getTotalAmount()) - safeLong(o.getDiscountAmount());
            if (min != null && total < min) return false;
            if (max != null && total > max) return false;

            return true;
        }).collect(Collectors.toList());

        Sort.Order order = parseSort(sort);
        Comparator<OrderAdminDTO> cmp;
        String prop = order.getProperty();
        boolean asc = order.getDirection().isAscending();

        switch (prop) {
            case "total" ->
                    cmp = Comparator.comparingLong(o -> safeLong(o.getTotalAmount()) - safeLong(o.getDiscountAmount()));
            case "status" ->
                    cmp = Comparator.comparing(o -> o.getStatus(), Comparator.nullsLast(String::compareTo));
            case "createdAt" ->
                    cmp = Comparator.comparing(o -> o.getCreatedAt(), Comparator.nullsLast(Comparator.naturalOrder()));
            default ->
                    cmp = Comparator.comparing(o -> o.getCreatedAt(), Comparator.nullsLast(Comparator.naturalOrder()));
        }
        list.sort(asc ? cmp : cmp.reversed());

        return list;
    }

    // helpers
    private long safeLong(Integer v) { return v == null ? 0L : v.longValue(); }
    private long safeLong(BigDecimal v) { return v == null ? 0L : v.longValue(); }

    public Page<OrderAdminDTO> getOrdersAdminPage(
            String q, String status, String payment,
            LocalDate from, LocalDate to,
            Long min, Long max,
            String sort, Integer page, Integer size
    ) {
        int p  = (page == null || page < 0) ? 0 : page;
        int sz = (size == null || size <= 0 || size > 200) ? 20 : size;

        Specification<Order> spec = (root, cq, cb) -> {
            List<Predicate> ps = new ArrayList<>();

            if (q != null && !q.isBlank()) {
                String like = "%" + q.trim().toLowerCase() + "%";
                ps.add(cb.or(
                        cb.like(cb.lower(root.get("status")), like),
                        cb.like(cb.lower(root.get("voucherCode")), like),
                        cb.like(cb.lower(root.get("note")), like),
                        cb.like(root.get("id").as(String.class), "%" + q.trim() + "%")
                ));
            }
            if (status != null && !status.isBlank()) {
                ps.add(cb.equal(root.get("status"), status));
            }
            if (payment != null && !payment.isBlank()) {
                ps.add(cb.equal(root.get("paymentMethod"), payment));
            }
            if (from != null) ps.add(cb.greaterThanOrEqualTo(root.get("createdAt"), from.atStartOfDay()));
            if (to != null)   ps.add(cb.lessThanOrEqualTo(root.get("createdAt"), to.atTime(23,59,59)));

            Expression<Number> finalTotal = cb.diff(
                    cb.coalesce(root.get("totalAmount"), BigDecimal.ZERO),
                    cb.coalesce(root.get("discountAmount"), BigDecimal.ZERO)
            );
            if (min != null) ps.add(cb.ge(finalTotal, min));
            if (max != null) ps.add(cb.le(finalTotal, max));

            return cb.and(ps.toArray(new Predicate[0]));
        };

        Sort.Order so = parseSort(sort);
        Pageable pageable = toPageable(p, sz, so);

        Page<Order> headerPage = orderRepository.findAll(spec, pageable);

        List<Long> ids = headerPage.getContent().stream().map(Order::getId).toList();
        Map<Long, List<OrderRepository.OrderItemFlat>> byOrder = ids.isEmpty()
                ? Collections.emptyMap()
                : orderRepository.findFlatByOrderIdsIncludingSoftDeleted(ids).stream()
                .collect(Collectors.groupingBy(OrderRepository.OrderItemFlat::getOrderId, LinkedHashMap::new, Collectors.toList()));

        List<OrderAdminDTO> content = headerPage.getContent().stream().map(o -> {
            List<OrderRepository.OrderItemFlat> flats = byOrder.getOrDefault(o.getId(), List.of());
            List<OrderItemDTO> items = flats.stream().map(r -> new OrderItemDTO(
                    r.getProductName() != null ? r.getProductName() : "[biến thể đã xoá]",
                    r.getColorName()   != null ? r.getColorName()   : "-",
                    r.getSizeLabel()   != null ? r.getSizeLabel()   : "-",
                    Optional.ofNullable(r.getQuantity()).orElse(0),
                    Optional.ofNullable(r.getPrice()).orElse(BigDecimal.ZERO)
            )).toList();

            return new OrderAdminDTO(
                    o.getId(),
                    o.getStatus(),
                    o.getPaymentMethod(),
                    o.getVoucherCode(),
                    o.getNote(),
                    o.getTotalAmount() == null ? 0 : o.getTotalAmount().intValue(),
                    o.getDiscountAmount(),
                    o.getShippingFee() == null ? 0 : o.getShippingFee().intValue(),
                    o.getCancelReason(),
                    o.getCreatedAt(),
                    items
            );
        }).toList();

        return new PageImpl<>(content, pageable, headerPage.getTotalElements());
    }

    private Sort.Order parseSort(String sort) {
        if (sort == null || sort.isBlank()) return Sort.Order.desc("createdAt");
        String[] s = sort.split(",");
        Sort.Direction dir = (s.length > 1 && "asc".equalsIgnoreCase(s[1])) ? Sort.Direction.ASC : Sort.Direction.DESC;
        return new Sort.Order(dir, s[0]);
    }

    private Pageable toPageable(int page, int size, Sort.Order so) {
        if ("total".equalsIgnoreCase(so.getProperty())) {
            Sort sort = org.springframework.data.jpa.domain.JpaSort
                    .unsafe(so.getDirection(), "(total_amount - COALESCE(discount_amount,0))");
            return PageRequest.of(page, size, sort);
        }
        return PageRequest.of(page, size, Sort.by(so));
    }

    @Transactional
    public Map<String, Object> applyVoucherToOrder(Long userId, Long orderId, String code) {
        Order order = orderRepository.findByIdAndUserId(orderId, userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Đơn không tồn tại"));

        if (!"PENDING".equals(order.getStatus())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Chỉ áp dụng mã khi đơn còn PENDING");
        }

        Voucher v = voucherRepository.findByCode(code)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Mã giảm giá không tồn tại"));

        LocalDate today = LocalDate.now();
        if (v.getStartDate() != null && v.getStartDate().isAfter(today))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Mã giảm giá chưa bắt đầu");
        if (v.getEndDate() != null && v.getEndDate().isBefore(today))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Mã giảm giá đã hết hạn");
        if (!Boolean.TRUE.equals(v.getIsActive()))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Mã giảm giá không khả dụng");
        if(!(v.getQuantity() >=1)){
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Mã giảm giá hết lượt");
        }
        // Subtotal lấy từ order_items (đơn giá đã là finalPrice sau sale)
        BigDecimal subtotal = computeOrderSubtotal(order.getId());

        if (v.getMinOrderAmount() != null && subtotal.compareTo(v.getMinOrderAmount()) < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Đơn chưa đạt giá trị tối thiểu áp dụng mã");
        }

        BigDecimal discount = computeDiscount(subtotal, v);

        // KHÔNG decrement ở đây!
        order.setVoucher(v);
        order.setVoucherCode(v.getCode());
        order.setDiscountAmount(discount);
        order.setUpdatedAt(LocalDateTime.now());
        orderRepository.save(order);

        return Map.of(
                "orderId", order.getId(),
                "voucherCode", v.getCode(),
                "discountAmount", discount
        );
    }

    private BigDecimal computeOrderSubtotal(Long orderId) {
        List<OrderItem> items = orderItemRepo.findByOrderId(orderId);
        return items.stream()
                .map(i -> Optional.ofNullable(i.getPrice()).orElse(BigDecimal.ZERO)
                        .multiply(BigDecimal.valueOf(i.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal computeDiscount(BigDecimal base, Voucher v) {
        BigDecimal discount = BigDecimal.ZERO;
        if ("fixed".equalsIgnoreCase(v.getDiscountType())) {
            discount = Optional.ofNullable(v.getDiscountValue()).orElse(BigDecimal.ZERO);
        } else if ("percent".equalsIgnoreCase(v.getDiscountType())) {
            BigDecimal pct = Optional.ofNullable(v.getDiscountValue()).orElse(BigDecimal.ZERO);
            discount = base.multiply(pct.divide(BigDecimal.valueOf(100)));
        }
        if (v.getMaxDiscountValue() != null && discount.compareTo(v.getMaxDiscountValue()) > 0) {
            discount = v.getMaxDiscountValue();
        }
        return discount.signum() < 0 ? BigDecimal.ZERO : discount;
    }

    @Transactional()
    public BigDecimal previewVoucherDiscount(String code, BigDecimal totalAfterSale) {
        // totalAfterSale = subtotal (sau sale, TRƯỚC voucher)
        BigDecimal total = totalAfterSale != null ? totalAfterSale : BigDecimal.ZERO;
        if (total.signum() <= 0) return BigDecimal.ZERO;

        Voucher voucher = voucherRepository.findByCode(code)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Mã giảm giá không tồn tại"));

        // Hiệu lực thời gian
        LocalDate today = LocalDate.now();
        boolean notStarted = voucher.getStartDate() != null && voucher.getStartDate().isAfter(today);
        boolean expired    = voucher.getEndDate()   != null && voucher.getEndDate().isBefore(today);
        if (notStarted || expired) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Mã giảm giá đã hết hạn hoặc chưa bắt đầu");
        }

        // Trạng thái khả dụng (preview không trừ lượt, nhưng vẫn chặn khi quantity <= 0)
        if (!Boolean.TRUE.equals(voucher.getIsActive())
                || voucher.getQuantity() == null
                || voucher.getQuantity() <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Mã giảm giá không khả dụng");
        }

        // Ngưỡng áp dụng
        if (voucher.getMinOrderAmount() != null && total.compareTo(voucher.getMinOrderAmount()) < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Đơn chưa đạt mức tối thiểu áp dụng mã");
        }

        // Tính tiền giảm
        BigDecimal discount = BigDecimal.ZERO;
        String type = voucher.getDiscountType() == null ? "" : voucher.getDiscountType().toLowerCase();

        if ("fixed".equals(type)) {
            discount = voucher.getDiscountValue() != null ? voucher.getDiscountValue() : BigDecimal.ZERO;
        } else if ("percent".equals(type)) {
            BigDecimal percent = voucher.getDiscountValue() != null ? voucher.getDiscountValue() : BigDecimal.ZERO; // ví dụ 10 = 10%
            // VND làm tròn 0 chữ số thập phân
            discount = total.multiply(percent).divide(BigDecimal.valueOf(100), 0, RoundingMode.HALF_UP);
        }

        // Giới hạn theo maxDiscountValue (nếu có)
        if (voucher.getMaxDiscountValue() != null && discount.compareTo(voucher.getMaxDiscountValue()) > 0) {
            discount = voucher.getMaxDiscountValue();
        }

        // Không vượt quá tổng, không âm, làm tròn 0 chữ số
        if (discount.compareTo(total) > 0) discount = total;
        if (discount.signum() < 0)        discount = BigDecimal.ZERO;

        return discount.setScale(0, RoundingMode.HALF_UP);
    }
}
