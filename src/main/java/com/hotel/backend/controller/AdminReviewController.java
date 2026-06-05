package com.hotel.backend.controller;

import com.hotel.backend.dto.ReviewResponseDTO;
import com.hotel.backend.model.User;
import com.hotel.backend.service.ReviewService;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

import java.util.List;

/**
 * Admin endpoints for managing reviews.
 * Provides full list, publish toggle, and delete operations.
 */
@RestController
@RequestMapping("/api/admin/review-management")
@RequiredArgsConstructor
public class AdminReviewController {

    private final ReviewService reviewService;
    private final com.hotel.backend.repository.UserRepository userRepository;

    /** Helper to fetch current authenticated user */
    private User getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getPrincipal() == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not authenticated");
        }
        String email = auth.getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found"));
    }

    /** Helper to verify admin role */
    private boolean isAdmin(User user) {
        return user.getRole() != null && "ADMIN".equalsIgnoreCase(user.getRole().getRoleName());
    }

    /** Get all reviews (admin) */
    @GetMapping
    public ResponseEntity<List<ReviewResponseDTO>> getAllReviews() {
        User current = getCurrentUser();
        if (!isAdmin(current)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied");
        }
        List<ReviewResponseDTO> list = reviewService.getAllReviews();
        return ResponseEntity.ok(list);
    }

    /** Toggle hide/show a review */
    @PatchMapping("/{reviewId}/publish")
    public ResponseEntity<ReviewResponseDTO> togglePublish(@PathVariable Long reviewId) {
        User current = getCurrentUser();
        if (!isAdmin(current)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied");
        }
        ReviewResponseDTO updated = reviewService.togglePublish(reviewId);
        return ResponseEntity.ok(updated);
    }

    /** Reply to a review */
    @PatchMapping("/{reviewId}/reply")
    public ResponseEntity<ReviewResponseDTO> replyToReview(
            @PathVariable Long reviewId,
            @RequestBody java.util.Map<String, String> body) {
        User current = getCurrentUser();
        if (!isAdmin(current)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied");
        }
        String reply = body.getOrDefault("reply", "");
        ReviewResponseDTO updated = reviewService.replyToReview(reviewId, reply);
        return ResponseEntity.ok(updated);
    }

    /** Delete a review */
    @DeleteMapping("/{reviewId}")
    public ResponseEntity<Void> deleteReview(@PathVariable Long reviewId) {
        User current = getCurrentUser();
        if (!isAdmin(current)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied");
        }
        reviewService.deleteReview(reviewId);
        return ResponseEntity.noContent().build();
    }
}
