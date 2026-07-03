package com.hotel.backend.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/**
 * DTO used when a user submits a review for a hotel.
 */
public record ReviewRequestDTO(
        Long bookingId,

        String comment,

        @Min(value = 1, message = "Rating must be at least 1")
        @Max(value = 5, message = "Rating cannot exceed 5")
        int rating
) {
}
