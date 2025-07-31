package api.rest.SeasFit.controller;

import api.rest.SeasFit.dto.ProductDetailDTO;
import api.rest.SeasFit.entity.Review;
import api.rest.SeasFit.security.JwtUtil;
import api.rest.SeasFit.service.ReviewService;
import api.rest.SeasFit.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/reviews")
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewService reviewService;
    private final JwtUtil jwtUtil;
    private final UserService userService;

    @PostMapping
    public ResponseEntity<?> addReview(@RequestBody ProductDetailDTO.ReviewDTO dto,
                                       @CookieValue(value = "jwtToken", required = false) String token) {
        if (token == null || token.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Thiếu token đăng nhập.");
        }

        String username = jwtUtil.extractUsername(token);
        if (username == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Token không hợp lệ.");
        }

        try {
            ProductDetailDTO.ReviewDTO savedDto = reviewService.saveReview(username, dto);
            return ResponseEntity.ok(savedDto);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Không thể lưu đánh giá: " + e.getMessage());
        }
    }


}
