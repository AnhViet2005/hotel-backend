package com.hotel.backend.model;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "booking_room_rates")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BookingRoomRate {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "booking_room_id")
    private BookingRoom bookingRoom;

    private LocalDate date;

    @Column(precision = 10, scale = 2)
    private BigDecimal price;
}
