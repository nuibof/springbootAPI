// api/rest/SeasFit/dto/UserAdminDTO.java
package api.rest.SeasFit.dto;

import lombok.Data;

import java.time.LocalDateTime;
public record UserAdminDTO(
        Long id,
        String userName,
        String fullName,
        String email,
        String phone,
        String role,
        String status,
        LocalDateTime createdAt,
        Long ordersCount,
        Long favoritesCount,
        Long commentsCount
) {}
