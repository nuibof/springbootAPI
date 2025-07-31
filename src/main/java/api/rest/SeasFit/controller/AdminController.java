package api.rest.SeasFit.controller;

import api.rest.SeasFit.dto.DashboardDTO;
import api.rest.SeasFit.dto.ProductAdminDTO;
import api.rest.SeasFit.entity.User;
import api.rest.SeasFit.service.OrderService;
import api.rest.SeasFit.service.ProductService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import api.rest.SeasFit.service.UserService;
import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final UserService userService;
    private final ProductService productService;
    private final OrderService orderService;

    public AdminController(UserService userService,ProductService productService1, OrderService orderService1) {
        this.userService = userService;
        this.productService = productService1;
        this.orderService = orderService1;
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


}
