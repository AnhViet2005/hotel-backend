package com.hotel.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PublicPostResponse {
    private Long id;
    private String title;
    private String subtitle;
    private String content;
    private String imageUrl;
    private Integer displayOrder;
    private java.util.List<PublicHotelResponse> hotels;
    private LocalDateTime createdAt;
}
