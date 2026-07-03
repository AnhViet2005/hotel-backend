package com.hotel.backend.service;

import com.hotel.backend.dto.ReviewRequestDTO;
import com.hotel.backend.dto.ReviewResponseDTO;
import com.hotel.backend.model.Hotel;
import com.hotel.backend.model.Review;
import com.hotel.backend.model.User;
import com.hotel.backend.repository.BookingRepository;
import com.hotel.backend.repository.ReviewRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Service handling review related operations.
 */
@Service
@RequiredArgsConstructor
@SuppressWarnings("null")
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final BookingRepository bookingRepository;
    private final NotificationService notificationService;

    @PersistenceContext
    private EntityManager entityManager;

    /**
     * Create a new review for the currently authenticated user.
     */
    @Transactional
    public ReviewResponseDTO createReview(User currentUser, ReviewRequestDTO request) {
        com.hotel.backend.model.Booking booking = bookingRepository.findById(request.bookingId())
                .orElseThrow(() -> new IllegalArgumentException("Booking not found"));
        
        if (!booking.getUser().getId().equals(currentUser.getId())) {
            throw new IllegalArgumentException("Không có quyền đánh giá đơn đặt phòng này");
        }
        
        if (booking.getStatus() != com.hotel.backend.model.Booking.BookingStatus.COMPLETED) {
            throw new IllegalArgumentException("Chỉ có thể đánh giá sau khi hoàn tất đơn đặt phòng");
        }

        Hotel hotel = booking.getHotel();
        if (hotel == null) {
            throw new IllegalArgumentException("Hotel not found for this booking");
        }

        Review review = Review.builder()
                .user(currentUser)
                .hotel(hotel)
                .booking(booking)
                .rating(java.math.BigDecimal.valueOf(request.rating()))
                .comment(request.comment())
                .isPublished(true)
                .build();
        Review saved = reviewRepository.save(review);
        
        // Gửi thông báo cho chủ khách sạn
        notificationService.notifyNewReview(saved);
        
        return toResponse(saved);
    }

    /**
     * Get reviews for a specific hotel.
     */
    @Transactional(readOnly = true)
    public List<ReviewResponseDTO> getReviewsByHotel(Long hotelId) {
        List<Review> reviews = reviewRepository.findByHotelIdAndIsPublishedTrue(hotelId);
        return reviews.stream().map(this::toResponse).collect(Collectors.toList());
    }

    /**
     * Get all reviews for hotels owned by a specific owner.
     */
    @Transactional(readOnly = true)
    public List<ReviewResponseDTO> getReviewsForOwner(User owner) {
        List<Review> reviews = reviewRepository.findByHotelOwnerId(owner.getId());
        return reviews.stream().map(this::toResponse).collect(Collectors.toList());
    }

    /**
     * Admin: get all reviews.
     */
    @Transactional(readOnly = true)
    public List<ReviewResponseDTO> getAllReviews() {
        return reviewRepository.findAll().stream().map(this::toResponse).collect(Collectors.toList());
    }

    /**
     * Admin: hide/show a review (toggle isPublished).
     */
    @Transactional
    public ReviewResponseDTO togglePublish(Long reviewId) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new IllegalArgumentException("Review not found"));
        review.setIsPublished(review.getIsPublished() == null ? false : !review.getIsPublished());
        Review saved = reviewRepository.save(review);
        return toResponse(saved);
    }

    /**
     * Admin: reply to a review.
     */
    @Transactional
    public ReviewResponseDTO replyToReview(Long reviewId, String replyText) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new IllegalArgumentException("Review not found"));
        review.setAdminReply(replyText);
        review.setAdminRepliedAt(java.time.LocalDateTime.now());
        Review saved = reviewRepository.save(review);
        return toResponse(saved);
    }

    /**
     * Owner: reply to a review for their own hotel.
     */
    @Transactional
    public ReviewResponseDTO replyToReviewAsOwner(User owner, Long reviewId, String replyText) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new IllegalArgumentException("Review not found"));
        
        if (!review.getHotel().getOwner().getId().equals(owner.getId())) {
            throw new IllegalArgumentException("Khách sạn này không phải của bạn");
        }

        review.setAdminReply(replyText);
        review.setAdminRepliedAt(java.time.LocalDateTime.now());
        Review saved = reviewRepository.save(review);
        return toResponse(saved);
    }

    /**
     * Admin: delete a review.
     */
    @Transactional
    public void deleteReview(Long reviewId) {
        if (!reviewRepository.existsById(reviewId)) {
            throw new IllegalArgumentException("Review not found");
        }
        reviewRepository.deleteById(reviewId);
    }

    /**
     * User: update their own review.
     */
    @Transactional
    public ReviewResponseDTO updateReview(User currentUser, Long reviewId, ReviewRequestDTO request) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new IllegalArgumentException("Review not found"));
        
        if (!review.getUser().getId().equals(currentUser.getId())) {
            throw new IllegalArgumentException("Bạn không có quyền sửa đánh giá của người khác");
        }

        review.setRating(java.math.BigDecimal.valueOf(request.rating()));
        review.setComment(request.comment());
        Review saved = reviewRepository.save(review);
        return toResponse(saved);
    }

    /**
     * User: delete their own review.
     */
    @Transactional
    public void deleteByUser(User currentUser, Long reviewId) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new IllegalArgumentException("Review not found"));
        
        if (!review.getUser().getId().equals(currentUser.getId())) {
            throw new IllegalArgumentException("Bạn không có quyền xóa đánh giá của người khác");
        }
        reviewRepository.delete(review);
    }

    private ReviewResponseDTO toResponse(Review review) {
        return new ReviewResponseDTO(
                review.getId(),
                review.getUser() != null ? review.getUser().getFullName() : null,
                review.getUser() != null ? review.getUser().getEmail() : null,
                review.getUser() != null ? review.getUser().getAvatarUrl() : null,
                review.getHotel() != null ? review.getHotel().getHotelName() : null,
                review.getRating() != null ? review.getRating().intValue() : 0,
                review.getComment(),
                review.getIsPublished(),
                review.getAdminReply(),
                review.getAdminRepliedAt(),
                review.getCreatedAt()
        );
    }
}
