package com.hotel.backend.dto;

import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;

@Data
@Builder
public class DashboardStatsResponse {
    private BigDecimal totalRevenue;
    private long totalBookings;
    private long totalCustomers;
    private long totalHotels;
}
