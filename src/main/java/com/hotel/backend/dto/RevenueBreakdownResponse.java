package com.hotel.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RevenueBreakdownResponse {
    private BigDecimal totalAmount;
    private BigDecimal adminRevenue;        // 30% - Platform commission
    private BigDecimal hotelOwnerRevenue;   // 70% - Hotel owner earnings
    private String revenueDistribution;     // "Admin: 30% | Hotel Owner: 70%"
}
