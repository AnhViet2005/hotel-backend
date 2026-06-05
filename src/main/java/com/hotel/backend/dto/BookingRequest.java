package com.hotel.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class BookingRequest {
    private Long hotelId;
    private Long roomTypeId;
    private LocalDate checkIn;
    private LocalDate checkOut;
    private String guestName;
    private String guestEmail;
    private String guestPhone;
    private Integer quantity;
}
