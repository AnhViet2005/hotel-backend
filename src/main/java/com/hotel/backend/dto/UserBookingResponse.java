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
public class UserBookingResponse {
    private Long id;
    private String bookingCode;
    private Long hotelId;
    private String hotelName;
    private String hotelImage;
    private String hotelCity;
    private String roomTypeName;
    private String guestName;
    private String guestPhone;
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
    
    private Boolean hasReview;
    private Integer reviewRating;
    private String reviewComment;
}
