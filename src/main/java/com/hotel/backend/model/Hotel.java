package com.hotel.backend.model;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Set;

@Entity
@Table(name = "hotels")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Hotel {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "hotel_name")
    private String hotelName;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "address_line")
    private String addressLine;

    private String city;
    private String district;
    private String ward;

    @Column(name = "star_rating", precision = 2, scale = 1)
    private BigDecimal starRating;

    @Column(length = 20)
    private String phone;

    @Column(length = 100)
    private String email;

    @Column(name = "base_price", precision = 12, scale = 2)
    private BigDecimal basePrice;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id")
    private User owner;

    @Column(name = "is_active")
    private Boolean isActive;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (isActive == null) isActive = true;
    }

    @OneToMany(mappedBy = "hotel", cascade = CascadeType.ALL)
    private Set<HotelImage> images;

    @OneToMany(mappedBy = "hotel", cascade = CascadeType.ALL)
    private Set<HotelAmenity> hotelAmenities;

    @OneToOne(mappedBy = "hotel", cascade = CascadeType.ALL)
    private HotelPolicy policy;

    @OneToMany(mappedBy = "hotel")
    private Set<RoomType> roomTypes;

    @OneToMany(mappedBy = "hotel")
    private Set<Booking> bookings;

    @OneToMany(mappedBy = "hotel")
    private Set<Review> reviews;
}
