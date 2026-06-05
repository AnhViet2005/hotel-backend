package com.hotel.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminBookingResponse {
    private Long id;
    private String bookingCode;
    private String customerName;
    private String customerEmail;
    private String hotelName;
    private LocalDate checkIn;
    private LocalDate checkOut;
    private BigDecimal totalAmount;
    private BigDecimal depositAmount;
    private BigDecimal remainingAmount;
    private String status;
    private String remainingPaymentStatus;
    private String remainingPaymentMethod;
    private LocalDateTime remainingPaidAt;
    private LocalDateTime createdAt;
}
