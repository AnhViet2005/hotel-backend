package com.hotel.backend.model;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.util.Set;

@Entity
@Table(name = "room_types")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RoomType {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "hotel_id")
    @com.fasterxml.jackson.annotation.JsonIgnore
    private Hotel hotel;

    @Column(name = "type_name")
    private String typeName;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "base_price", precision = 12, scale = 2)
    private BigDecimal basePrice;

    @Column(name = "max_adults")
    private Integer maxAdults;

    @Column(name = "max_children")
    private Integer maxChildren;

    @Column(name = "total_rooms")
    private Integer totalRooms;

    @Column(name = "room_size")
    private Double roomSize;

    @OneToMany(mappedBy = "roomType", cascade = CascadeType.ALL)
    private Set<RoomImage> images;

    @OneToMany(mappedBy = "roomType", cascade = CascadeType.ALL)
    private Set<RoomCalendar> calendarDays;

    @OneToMany(mappedBy = "roomType")
    private Set<BookingRoom> bookingRooms;
}
