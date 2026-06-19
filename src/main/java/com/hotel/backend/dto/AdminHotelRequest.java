package com.hotel.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminHotelRequest {
    private String name;
    private String description;
    private String addressLine;
    private String city;
    private String district;
    private String ward;
    private BigDecimal rating;
    private String phone;
    private String email;
    private Boolean isActive;
    private BigDecimal basePrice;
    private Integer depositPercentage;
    private String imageUrl; // For simplicity in this assignment, just one image URL
}
