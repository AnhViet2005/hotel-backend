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
public class AdminHotelResponse {
    private Long id;
    private String name;
    private String location;
    private BigDecimal rating;
    private String price;
    private BigDecimal basePrice;
    private String status;
    private Integer rooms;
    private String image;
    private String description;
    private String addressLine;
    private String phone;
    private String email;
    private Integer depositPercentage;
}
