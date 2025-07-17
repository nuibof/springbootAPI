package api.rest.SeasFit.service;

import api.rest.SeasFit.entity.Review;
import api.rest.SeasFit.repository.ReviewRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ReviewService {

    private final ReviewRepository reviewRepository;

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

    // Avg rating for a product
    public double avgRatingByProductId(Long productId) {
        Double reviews = reviewRepository.avgRatingByProductId(productId);
        return reviews == null ? 0 : reviews.doubleValue();
    }
}
