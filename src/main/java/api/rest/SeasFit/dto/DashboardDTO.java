package api.rest.SeasFit.dto;

import api.rest.SeasFit.entity.User;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DashboardDTO {
    private int totalUsers;
    private int totalProducts;
    private int totalOrders;
    private int lowStockProducts;
    private List<User> latestUsers;
}
