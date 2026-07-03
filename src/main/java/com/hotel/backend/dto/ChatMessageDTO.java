package com.hotel.backend.dto;

import java.time.LocalDateTime;

public record ChatMessageDTO(
    Long id,
    Long senderId,
    String senderName,
    Long receiverId,
    String content,
    Boolean isRead,
    LocalDateTime createdAt
) {}
