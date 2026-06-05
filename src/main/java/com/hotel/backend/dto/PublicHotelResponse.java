package com.hotel.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PublicHotelResponse {
    private Long id;
    private String name;
    private String location;   // city
    private String address;    // addressLine + district
    private BigDecimal price;  // min base price
    private BigDecimal rating;
    private Integer reviews;
    private String image;      // primary image
    private List<String> gallery;
    private String description;
    private List<String> amenities;
    private List<PublicRoomTypeResponse> rooms;
}
