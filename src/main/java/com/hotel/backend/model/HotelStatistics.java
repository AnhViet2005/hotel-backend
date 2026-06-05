package com.hotel.backend.model;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "hotel_statistics")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HotelStatistics {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "hotel_id")
    private Hotel hotel;

    @Column(name = "stat_date")
    private LocalDate statDate;

    @Column(name = "total_bookings")
    private Integer totalBookings;

    @Column(name = "total_revenue", precision = 12, scale = 2)
    private BigDecimal totalRevenue;
}
