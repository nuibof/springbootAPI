package api.rest.SeasFit.controller;

import api.rest.SeasFit.dto.DashboardDTO;
import api.rest.SeasFit.service.OrderService;
import api.rest.SeasFit.service.ProductService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import api.rest.SeasFit.service.UserService;

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
        try{
            int totalUsers = userService.getTotalUser();
            int totalProducts = productService.getTotalProducts();
            int totalOrders = orderService.getTotalOrders();
            int lowStockProducts = productService.getLowStockProducts();

            DashboardDTO dto = new DashboardDTO(totalUsers, totalProducts, totalOrders, lowStockProducts);
            return ResponseEntity.ok(dto);
        }catch (Exception e){
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }

    }
}
