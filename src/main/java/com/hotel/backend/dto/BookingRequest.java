package com.hotel.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class BookingRequest {
    private Long hotelId;
    private Long roomTypeId; // Kept for legacy/single room support
    private List<RoomSelection> rooms; // New: support multiple rooms at once
    private LocalDate checkIn;
    private LocalDate checkOut;
    private String guestName;
    private String guestEmail;
    private String guestPhone;
    private Integer quantity; // Kept for legacy
    private String promoCode;
}
