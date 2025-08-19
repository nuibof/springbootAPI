package api.rest.SeasFit.controller;

import api.rest.SeasFit.dto.DashboardDTO;
import api.rest.SeasFit.dto.OrderAdminDTO;
import api.rest.SeasFit.dto.ProductAdminDTO;
import api.rest.SeasFit.entity.Revenue;
import api.rest.SeasFit.entity.User;
import api.rest.SeasFit.repository.RevenueRepository;
import api.rest.SeasFit.service.OrderService;
import api.rest.SeasFit.service.ProductService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import api.rest.SeasFit.service.UserService;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final UserService userService;
    private final ProductService productService;
    private final OrderService orderService;
    private final RevenueRepository revenueRepo;

    public AdminController(UserService userService, ProductService productService1, OrderService orderService1, RevenueRepository revenueRepo) {
        this.userService = userService;
        this.productService = productService1;
        this.orderService = orderService1;
        this.revenueRepo = revenueRepo;
    }

    @GetMapping("/dashboard")
    public ResponseEntity<DashboardDTO> getDashboard() {
        try {
            int totalUsers = userService.getTotalUser();
            int totalProducts = productService.getTotalProducts();
            int totalOrders = orderService.getTotalOrders();
            int lowStockProducts = productService.getLowStockProducts();

            List<User> latestUsers = userService.getLatestUsers(5);
            if (latestUsers == null) latestUsers = new ArrayList<>();

            System.out.println(">>> latestUsers: " + latestUsers.size());
            DashboardDTO dto = new DashboardDTO(
                    totalUsers, totalProducts, totalOrders, lowStockProducts, latestUsers
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

    @GetMapping("/orders")
    public ResponseEntity<List<OrderAdminDTO>> getAllOrdersForAdmin() {
        List<OrderAdminDTO> orders = orderService.getAllOrdersWithItems();
        return ResponseEntity.ok(orders);
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


}
