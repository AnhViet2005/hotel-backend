package com.hotel.backend.dto;

import java.time.LocalDateTime;

/**
 * DTO representing a review response.
 */
public record ReviewResponseDTO(
        Long id,
        String userName,
        String userAvatar,
        String hotelName,
        int rating,
        String comment,
        Boolean isPublished,
        String adminReply,
        LocalDateTime adminRepliedAt,
        LocalDateTime createdAt
) {
}
