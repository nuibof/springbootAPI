// src/main/java/api/rest/SeasFit/service/ReviewService.java
package api.rest.SeasFit.service;

import api.rest.SeasFit.dto.ProductDetailDTO;
import api.rest.SeasFit.entity.Review;
import api.rest.SeasFit.entity.User;
import api.rest.SeasFit.repository.ReviewRepository;
import api.rest.SeasFit.repository.OrderItemRepository; // <-- thêm
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final UserService userService;
    private final OrderItemRepository orderItemRepository; // <-- thêm

    public List<Review> findAll() { return reviewRepository.findAll(); }
    public Optional<Review> findById(Long id) { return reviewRepository.findById(id); }
    public Review save(Review entity) { return reviewRepository.save(entity); }
    public void deleteById(Long id) { reviewRepository.deleteById(id); }

    public double avgRatingByProductId(Long productId) {
        Double reviews = reviewRepository.avgRatingByProductId(productId);
        return reviews == null ? 0 : reviews.doubleValue();
    }

    public ProductDetailDTO.ReviewDTO saveReview(String username, ProductDetailDTO.ReviewDTO dto) {
        User user = userService.findByUserName(username);
        if (user == null) throw new RuntimeException("User not found");

        Long userId = user.getId();
        Long productId = dto.getId();

        // 1) Bắt buộc đã mua và đã giao
        boolean purchased = orderItemRepository.hasDeliveredPurchase(userId, productId);
        if (!purchased) {
            throw new IllegalStateException("Bạn chỉ có thể đánh giá các sản phẩm đã mua và đã giao (DELIVERED).");
        }

        // 2) Chặn review trùng
        if (reviewRepository.existsByUserIdAndProductId(userId, productId)) {
            throw new IllegalStateException("Bạn đã đánh giá sản phẩm này rồi.");
        }

        // 3) Validate đơn giản rating (1..5)
        Integer rating = dto.getRating();
        if (rating == null || rating < 1 || rating > 5) {
            throw new IllegalArgumentException("Điểm đánh giá không hợp lệ (1–5).");
        }

        // 4) Lưu review
        Review review = new Review();
        review.setUserId(userId);
        review.setProductId(productId);
        review.setRating(rating);
        review.setComment(dto.getContent());
        review.setCreatedAt(LocalDateTime.now());
        reviewRepository.save(review);

        // 5) Build DTO trả về
        ProductDetailDTO.ReviewDTO responseDto = new ProductDetailDTO.ReviewDTO();
        responseDto.setId(review.getId());
        responseDto.setContent(review.getComment());
        responseDto.setRating(review.getRating());
        responseDto.setUserName(user.getFullName());
        responseDto.setCreatedAt(review.getCreatedAt());
        return responseDto;
    }
}
