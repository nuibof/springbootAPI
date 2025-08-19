package api.rest.SeasFit.controller;

import api.rest.SeasFit.dto.ProductDetailDTO;
import api.rest.SeasFit.security.JwtUtil;
import api.rest.SeasFit.service.ReviewService;
import api.rest.SeasFit.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/reviews")
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewService reviewService;
    private final JwtUtil jwtUtil;
    private final UserService userService;

    private ResponseEntity<?> unauthorized(String msg) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(msg);
    }

    @PostMapping
    public ResponseEntity<?> addReview(
            @RequestBody ProductDetailDTO.ReviewDTO dto,
            @RequestHeader(value = "Authorization", required = false) String authHeader
    ) {
        try {
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return unauthorized("Thiếu token xác thực");
            }
            String token = authHeader.substring(7);
            String username = jwtUtil.extractUsername(token);
            if (username == null) {
                return unauthorized("Token không hợp lệ");
            }

            // Nếu cần kiểm tra user tồn tại:
            if (userService.findByUserName(username) == null) {
                return unauthorized("Người dùng không tồn tại");
            }

            ProductDetailDTO.ReviewDTO savedDto = reviewService.saveReview(username, dto);
            return ResponseEntity.ok(savedDto);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Không thể lưu đánh giá: " + e.getMessage());
        }
    }
}
