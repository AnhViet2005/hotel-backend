package com.hotel.backend.controller;

import com.hotel.backend.dto.ReviewRequestDTO;
import com.hotel.backend.dto.ReviewResponseDTO;
import com.hotel.backend.model.User;
import com.hotel.backend.service.ReviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

import jakarta.validation.Valid;
import java.util.List;

/**
 * Public endpoints for handling hotel reviews.
 * Users can submit a review and view reviews for a hotel.
 */
@RestController
@RequestMapping("/api/reviews")
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewService reviewService;
    private final com.hotel.backend.repository.UserRepository userRepository;

    /** Helper to fetch the currently authenticated user */
    private User getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getPrincipal() == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not authenticated");
        }
        String email = auth.getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found"));
    }

    /** Submit a new review (user side) */
    @PostMapping
    public ResponseEntity<ReviewResponseDTO> createReview(@Valid @RequestBody ReviewRequestDTO request) {
        User current = getCurrentUser();
        ReviewResponseDTO response = reviewService.createReview(current, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /** Get all reviews for a given hotel (public) */
    @GetMapping("/hotel/{hotelId}")
    public ResponseEntity<List<ReviewResponseDTO>> getReviewsByHotel(@PathVariable Long hotelId) {
        List<ReviewResponseDTO> list = reviewService.getReviewsByHotel(hotelId);
        return ResponseEntity.ok(list);
    }
    
    /** Get reviews for hotels owned by current owner */
    @GetMapping("/owner")
    public ResponseEntity<List<ReviewResponseDTO>> getOwnerReviews() {
        User current = getCurrentUser();
        List<ReviewResponseDTO> list = reviewService.getReviewsForOwner(current);
        return ResponseEntity.ok(list);
    }
    
    /** Owner: Reply to a review */
    @PatchMapping("/{reviewId}/owner-reply")
    public ResponseEntity<ReviewResponseDTO> ownerReply(
            @PathVariable Long reviewId,
            @RequestBody java.util.Map<String, String> body) {
        User current = getCurrentUser();
        String reply = body.getOrDefault("reply", "");
        ReviewResponseDTO updated = reviewService.replyToReviewAsOwner(current, reviewId, reply);
        return ResponseEntity.ok(updated);
    }

    /** Update a review (user side) */
    @PutMapping("/{id}")
    public ResponseEntity<ReviewResponseDTO> updateReview(@PathVariable Long id, @Valid @RequestBody ReviewRequestDTO request) {
        User current = getCurrentUser();
        ReviewResponseDTO response = reviewService.updateReview(current, id, request);
        return ResponseEntity.ok(response);
    }

    /** Delete a review (user side) */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteReview(@PathVariable Long id) {
        User current = getCurrentUser();
        reviewService.deleteByUser(current, id);
        return ResponseEntity.noContent().build();
    }
}
