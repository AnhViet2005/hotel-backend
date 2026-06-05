package com.hotel.backend.dto;

import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class RecentBookingResponse {
    private String id;
    private String customerName;
    private String hotelName;
    private LocalDateTime bookingDate;
    private BigDecimal amount;
    private BigDecimal depositAmount;
    private BigDecimal remainingAmount;
    private String status;
}
