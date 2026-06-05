package com.hotel.backend.model;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "room_calendar")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RoomCalendar {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "room_type_id")
    private RoomType roomType;

    private LocalDate date;

    @Column(precision = 10, scale = 2)
    private BigDecimal price;

    @Column(name = "total_rooms")
    private Integer totalRooms;

    @Column(name = "booked_rooms")
    private Integer bookedRooms;

    @Column(name = "is_available")
    private Boolean isAvailable;
}
