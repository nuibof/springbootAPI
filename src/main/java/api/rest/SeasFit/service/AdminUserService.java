// api/rest/SeasFit/service/AdminUserService.java
package api.rest.SeasFit.service;

import api.rest.SeasFit.dto.OrderAdminDTO;
import api.rest.SeasFit.dto.UserAdminDTO;
import api.rest.SeasFit.entity.Order;
import api.rest.SeasFit.entity.User;
import api.rest.SeasFit.repository.FavoriteRepository;
import api.rest.SeasFit.repository.OrderRepository;
import api.rest.SeasFit.repository.ReviewRepository;
import api.rest.SeasFit.repository.UserRepository;
import jakarta.transaction.Transactional;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

@Service
@Transactional
public class AdminUserService {

    private final UserRepository userRepo;
    private final OrderRepository orderRepo;
    private final FavoriteRepository favoriteRepo;
    private final ReviewRepository reviewRepo; // <— camelCase đúng

    public AdminUserService(UserRepository userRepo,
                            OrderRepository orderRepo,
                            FavoriteRepository favoriteRepo,
                            ReviewRepository reviewRepo) {
        this.userRepo = userRepo;
        this.orderRepo = orderRepo;
        this.favoriteRepo = favoriteRepo;
        this.reviewRepo = reviewRepo;
    }

    public Page<UserAdminDTO> searchUsers(String keyword,
                                          String role,
                                          String status,
                                          int page,
                                          int size,
                                          String sort) {

        Sort sortObj = parseSort(sort); // ví dụ "createdAt,desc"
        Pageable pageable = PageRequest.of(page, size, sortObj);

        Specification<User> spec = Specification.where(null);

        if (keyword != null && !keyword.isBlank()) {
            String kw = "%" + keyword.trim().toLowerCase(Locale.ROOT) + "%";
            spec = spec.and((root, q, cb) -> cb.or(
                    cb.like(cb.lower(root.get("userName")), kw),
                    cb.like(cb.lower(root.get("fullName")), kw),
                    cb.like(cb.lower(root.get("email")), kw),
                    cb.like(cb.lower(root.get("phone")), kw)
            ));
        }
        if (role != null && !role.isBlank()) {
            spec = spec.and((root, q, cb) -> cb.equal(root.get("role"), role));
        }
        if (status != null && !status.isBlank()) {
            spec = spec.and((root, q, cb) -> cb.equal(root.get("status"), status));
        }

        Page<User> pageUsers = userRepo.findAll(spec, pageable);

        // ⚠️ map đúng THỨ TỰ record: id, userName, fullName, email, phone, role, status, createdAt, ordersCount, favoritesCount, commentsCount
        return pageUsers.map(u -> new UserAdminDTO(
                u.getId(),
                u.getUserName(),
                u.getFullName(),
                u.getEmail(),
                u.getPhone(),
                u.getRole(),
                safeStatus(u.getStatus()),
                u.getCreatedAt(),
                orderRepo.countByUserId(u.getId()),
                favoriteRepo.countByUserId(u.getId()),
                reviewRepo.countByUserId(u.getId())
        ));
    }

    public UserAdminDTO getUserById(Long id) {
        User u = userRepo.findById(id).orElseThrow(() -> new RuntimeException("User not found"));
        return toUserAdminDTO(u);
    }

    public Page<OrderAdminDTO> getUserOrders(Long userId, Pageable pageable) {
        return orderRepo.findByUserId(userId, pageable).map(this::toOrderAdminDTO);
    }

    public Map<String, Object> getUserStats(Long userId) {
        long orders = orderRepo.countByUserId(userId);
        long favs   = favoriteRepo != null ? favoriteRepo.countByUserId(userId) : 0;
        long cmts   = reviewRepo  != null ? reviewRepo.countByUserId(userId)  : 0;
        Map<String,Object> m = new LinkedHashMap<>();
        m.put("ordersCount", orders);
        m.put("favoritesCount", favs);
        m.put("commentsCount", cmts);
        return m;
    }

    // BE đang dùng String status: "ACTIVE"/"INACTIVE"
    public void updateUserActive(Long userId, boolean active) {
        User u = userRepo.findById(userId).orElseThrow(() -> new RuntimeException("User not found"));
        u.setStatus(active ? "ACTIVE" : "INACTIVE");
        userRepo.save(u);
    }

    public void updateUserRole(Long userId, String role) {
        if (!"ROLE_ADMIN".equals(role) && !"ROLE_USER".equals(role))
            throw new IllegalArgumentException("Role không hợp lệ");
        User u = userRepo.findById(userId).orElseThrow(() -> new RuntimeException("User not found"));
        u.setRole(role);
        userRepo.save(u);
    }

    public Page<OrderAdminDTO> recentOrders(Long userId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Order> orders = orderRepo.findByUserId(userId, pageable);

        return orders.map(o -> new OrderAdminDTO(
                o.getId(),
                o.getStatus(),
                o.getPaymentMethod(),
                o.getVoucher() != null ? o.getVoucher().getCode() : null,
                o.getNote(),
                o.getTotalAmount(),
                o.getDiscountAmount(),
                o.getShippingFee(),
                o.getCancelReason(),
                o.getCreatedAt(),
                /* items */ null
        ));
    }

    private Sort parseSort(String sort) {
        if (sort == null || sort.isBlank()) return Sort.by(Sort.Direction.DESC, "createdAt");
        String[] p = sort.split(",");
        String prop = (p.length > 0 && !p[0].isBlank()) ? p[0] : "createdAt";
        Sort.Direction dir = (p.length > 1 && "asc".equalsIgnoreCase(p[1])) ? Sort.Direction.ASC : Sort.Direction.DESC;
        return Sort.by(new Sort.Order(dir, prop));
    }

    private UserAdminDTO toUserAdminDTO(User u) {
        long orders = orderRepo != null ? orderRepo.countByUserId(u.getId()) : 0L;
        long favs   = favoriteRepo != null ? favoriteRepo.countByUserId(u.getId()) : 0L;
        long cmts   = reviewRepo  != null ? reviewRepo.countByUserId(u.getId())  : 0L;

        // ⚠️ map ĐÚNG THỨ TỰ record (có cả phone ở vị trí thứ 5)
        return new UserAdminDTO(
                u.getId(),
                u.getUserName(),
                u.getFullName(),
                u.getEmail(),
                u.getPhone(),
                u.getRole(),
                safeStatus(u.getStatus()),
                u.getCreatedAt(),
                orders,
                favs,
                cmts
        );
    }

    private String safeStatus(String s) {
        if (s == null) return "INACTIVE";
        String up = s.trim().toUpperCase();
        return ("ACTIVE".equals(up) || "INACTIVE".equals(up)) ? up : "INACTIVE";
    }

    private OrderAdminDTO toOrderAdminDTO(Order o) {
        OrderAdminDTO dto = new OrderAdminDTO();
        dto.setId(o.getId());
        dto.setCreatedAt(o.getCreatedAt());   // ✅ đúng field
        dto.setStatus(o.getStatus());
        dto.setTotalAmount(o.getTotalAmount()); // chú ý: DTO dùng Integer, entity có thể là int/Integer
        dto.setPaymentMethod(o.getPaymentMethod());
        dto.setVoucherCode(o.getVoucher() != null ? o.getVoucher().getCode() : null);
        dto.setNote(o.getNote());
        dto.setDiscountAmount(o.getDiscountAmount());
        dto.setShippingFee(o.getShippingFee());
        dto.setCancelReason(o.getCancelReason());
        // items nếu cần map thì map thêm
        return dto;
    }


    public void updateUserStatus(Long userId, String status) {
        User u = userRepo.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        u.setStatus(safeStatus(status));
        userRepo.save(u);
    }
}
