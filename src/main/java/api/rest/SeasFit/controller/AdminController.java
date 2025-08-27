package api.rest.SeasFit.controller;

import api.rest.SeasFit.dto.*;
import api.rest.SeasFit.entity.Category;
import api.rest.SeasFit.entity.Revenue;
import api.rest.SeasFit.entity.User;
import api.rest.SeasFit.repository.RevenueRepository;
import api.rest.SeasFit.service.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@PreAuthorize("hasRole('ADMIN')")
@RequestMapping("/api/admin")
public class AdminController {

    private final UserService userService;
    private final ProductService productService;
    private final OrderService orderService;
    private final RevenueRepository revenueRepo;
    private final CategoryService categoryService;
    private final AdminUserService adminUserService;
    private final FavoriteService favoriteService;
    private final VoucherService voucherService;

    public AdminController(UserService userService, ProductService productService1, OrderService orderService1, RevenueRepository revenueRepo, CategoryService categoryService, AdminUserService adminUserService, FavoriteService favoriteService, VoucherService voucherService) {
        this.userService = userService;
        this.productService = productService1;
        this.orderService = orderService1;
        this.revenueRepo = revenueRepo;
        this.categoryService = categoryService;
        this.adminUserService = adminUserService;
        this.favoriteService = favoriteService;
        this.voucherService = voucherService;
    }

    @GetMapping("/dashboard")
    public ResponseEntity<DashboardDTO> getDashboard() {
        try {
            int totalUsers = userService.getTotalUser();
            int totalProducts = productService.getTotalProducts();
            int totalOrders = orderService.getTotalOrders();
            int lowStockProducts = productService.getLowStockProducts();
            int totalVouchers = Math.toIntExact(voucherService.countVouchers());
            List<User> latestUsers = userService.get5LatestUsers();
            if (latestUsers == null) latestUsers = new ArrayList<>();

            System.out.println(">>> latestUsers: " + latestUsers.size());
            DashboardDTO dto = new DashboardDTO(
                    totalUsers, totalProducts, totalOrders, lowStockProducts,totalVouchers, latestUsers
            );
            return ResponseEntity.ok(dto);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }

    @GetMapping("/products")
    public Page<ProductAdminDTO> getProductsForAdmin(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) Integer gender,
            Pageable pageable
    ) {
        return productService.searchAdminProducts(keyword, status, categoryId, gender, pageable);
    }



    @DeleteMapping("/products/{id}")
    public ResponseEntity<String> deleteProduct(@PathVariable Long id) {
        try {
            productService.deleteById(id);
            return ResponseEntity.ok("Product deleted successfully.");
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error deleting product.");
        }
    }

    @PutMapping("products/{id}/sale")
    public ResponseEntity<Void> updateSale(@PathVariable("id") Long productId,
                                           @RequestBody UpdateSaleRequest request) {
        productService.updateSaleForProduct(productId, request);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/orders")
    public ResponseEntity<Page<OrderAdminDTO>> getOrdersForAdmin(
            @RequestParam(required = false) String q,                 // search: id/status/voucher/note
            @RequestParam(required = false) String status,            // PENDING/PAID/SHIPPING/DELIVERED/CANCELLED
            @RequestParam(required = false) String payment,           // COD/VNPAY/MOMO/BANKING...
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) Long min,                 // min total (VND)
            @RequestParam(required = false) Long max,                 // max total (VND)
            @RequestParam(defaultValue = "createdAt,desc") String sort, // createdAt|total|status, asc|desc
            @RequestParam(defaultValue = "0") Integer page,
            @RequestParam(defaultValue = "20") Integer size
    ) {
        Page<OrderAdminDTO> result = orderService.getOrdersAdminPage(
                q, status, payment, from, to, min, max, sort, page, size
        );
        return ResponseEntity.ok(result);
    }

    @PatchMapping("orders/{orderId}/status")
    public ResponseEntity<?> updateOrderStatus(
            @PathVariable Long orderId,
            @RequestBody Map<String, String> payload) {

        String newStatus = payload.get("status");
        if (newStatus == null || newStatus.isBlank()) {
            return ResponseEntity.badRequest().body("Trạng thái mới không hợp lệ.");
        }

        try {
            orderService.updateOrderStatus(orderId, newStatus);
            return ResponseEntity.ok("Cập nhật trạng thái đơn hàng thành công.");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Lỗi: " + e.getMessage());
        }
    }

    @GetMapping("/revenues")
    public List<Map<String, Object>> getAllRevenues() {
        return revenueRepo.findAll().stream().map(r -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("orderId",  r.getOrder() != null ? r.getOrder().getId() : null);
            m.put("amount",   r.getAmount());
            m.put("createdAt", r.getCreatedAt());
            return m;
        }).toList(); // hoặc .collect(Collectors.toList())
    }


    @PostMapping("/categories/add")
    public Category addCategory(Category category) {
        return categoryService.save(category);
    }
    @PutMapping("/categories/{id}")
    public Category updateCategory(@PathVariable Long id, @RequestBody Category updated) {
        Category existing = categoryService.findById(id).orElseThrow(() -> new RuntimeException("Not found"));
        existing.setName(updated.getName());
        return categoryService.save(existing);
    }

    @DeleteMapping("/categories/{id}")
    public ResponseEntity<?> delete(@PathVariable Long id) {
        categoryService.deleteById(id);
        return ResponseEntity.ok("Deleted");
    }

    @GetMapping("/categories/all")
    public List<Category> getAllCategories() {
        return categoryService.findAll();
    }

    // ========== USERS DETAIL ==========

    // Lấy 1 user theo id (cho modal)
    @GetMapping("/users/{id}")
    public ResponseEntity<UserAdminDTO> getUser(@PathVariable Long id) {
        return ResponseEntity.ok(adminUserService.getUserById(id));
    }

    // Lấy đơn gần đây của user (size mặc định 5)
    @GetMapping("/users/{id}/orders")
    public ResponseEntity<Page<OrderAdminDTO>> getUserOrders(
            @PathVariable Long id,
            Pageable pageable // dùng page,size,sort client truyền lên
    ) {
        return ResponseEntity.ok(adminUserService.getUserOrders(id, pageable));
    }

    // Thống kê nhanh (orders/favorites/comments) cho modal
    @GetMapping("/users/{id}/stats")
    public ResponseEntity<Map<String, Object>> getUserStats(@PathVariable Long id) {
        Map<String, Object> stats = adminUserService.getUserStats(id);
        return ResponseEntity.ok(stats);
    }

    // Đổi role: ROLE_USER <-> ROLE_ADMIN
    @PatchMapping("/users/{id}/role")
    public ResponseEntity<?> updateUserRole(@PathVariable Long id, @RequestBody Map<String, String> payload
    ) {
        String role = payload.get("role");
        if (role == null || role.isBlank()) {
            return ResponseEntity.badRequest().body("role null/blank");
        }
        adminUserService.updateUserRole(id, role.trim());
        return ResponseEntity.ok("OK");
    }

    // Bật/tắt active (nhận cả 2 kiểu cho “đỡ khổ” UI): { active: true/false } HOẶC { status: 'ACTIVE'/'INACTIVE' }
    @PatchMapping("/users/{id}/status")
    public ResponseEntity<?> updateUserStatus(@PathVariable Long id, @RequestBody Map<String, String> payload
    ) {
        String newStatus = payload.get("status");
        if (newStatus == null || newStatus.isBlank()) {
            return ResponseEntity.badRequest().body("Thiếu status");
        }

        newStatus = newStatus.trim().toUpperCase();
        if (!newStatus.equals("ACTIVE") && !newStatus.equals("INACTIVE")) {
            return ResponseEntity.badRequest().body("Status phải là ACTIVE hoặc INACTIVE");
        }

        adminUserService.updateUserStatus(id, newStatus);
        return ResponseEntity.ok("OK");
    }



    // GET /api/admin/users/page?keyword=&role=&status=&page=0&size=10&sort=createdAt,desc
    @GetMapping("/users/page")
    public Page<UserAdminDTO> getUsersPage(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String role,
            @RequestParam(required = false) String status,
            @RequestParam(required = false, defaultValue = "createdAt,desc") String sort
    ) {
        return adminUserService.searchUsers(keyword, role, status, page, size, sort);
    }
    // ========== FAVORITE STATS ========== GET /api/admin/stats?q=&page=0&size=20

    @GetMapping("/favorites/stats")
    public Page<FavoriteAdminDTO> stats(@RequestParam(required = false) String q, @RequestParam(defaultValue = "0") Integer page,
                                        @RequestParam(defaultValue = "20") Integer size
    ) {
        return favoriteService.pageFavoriteStats(q, page, size);
    }

}
