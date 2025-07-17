package api.rest.SeasFit.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DashboardDTO {
    private int totalUsers;
    private int totalProducts;
    private int totalOrders;
    private int lowStockProducts;
}
