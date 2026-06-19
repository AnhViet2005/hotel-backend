package com.hotel.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PublicRoomTypeResponse {
    private Long id;
    private String name;
    private Integer capacity;
    private String size;
    private String bedType;
    private BigDecimal price;
    private List<String> features;
    private String image;
    private List<String> imageUrls;
    private String description;
    /**
     * Số phòng còn khả dụng cho loại phòng này.
     * Được tính trong PublicHotelService và trả về tới frontend.
     */
    @JsonProperty("availableRooms")
    private Integer availableRooms;

    @JsonProperty("nextAvailableDate")
    private String nextAvailableDate;
}
