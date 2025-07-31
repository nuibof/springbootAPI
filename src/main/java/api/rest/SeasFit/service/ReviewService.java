package api.rest.SeasFit.service;

import api.rest.SeasFit.dto.ProductDetailDTO;
import api.rest.SeasFit.entity.Review;
import api.rest.SeasFit.entity.User;
import api.rest.SeasFit.repository.ReviewRepository;
import api.rest.SeasFit.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final UserService userService;
    private final JwtUtil jwtUtil;

    public List<Review> findAll() {
        return reviewRepository.findAll();
    }

    public Optional<Review> findById(Long id) {
        return reviewRepository.findById(id);
    }

    public Review save(Review entity) {
        return reviewRepository.save(entity);
    }

    public void deleteById(Long id) {
        reviewRepository.deleteById(id);
    }

    public double avgRatingByProductId(Long productId) {
        Double reviews = reviewRepository.avgRatingByProductId(productId);
        return reviews == null ? 0 : reviews.doubleValue();
    }

    public ProductDetailDTO.ReviewDTO saveReview(String username, ProductDetailDTO.ReviewDTO dto) {
        User user = userService.findByUserName(username);
        if (user == null) {
            throw new RuntimeException("User not found");
        }

        Review review = new Review();
        review.setUserId(user.getId());
        review.setProductId(dto.getId());
        review.setRating(dto.getRating());
        review.setComment(dto.getContent());
        review.setCreatedAt(LocalDateTime.now());

        reviewRepository.save(review);

        ProductDetailDTO.ReviewDTO responseDto = new ProductDetailDTO.ReviewDTO();
        responseDto.setId(review.getId());
        responseDto.setContent(review.getComment());
        responseDto.setRating(review.getRating());
        responseDto.setUserName(user.getFullName());
        responseDto.setCreatedAt(review.getCreatedAt());

        return responseDto;
    }



}
