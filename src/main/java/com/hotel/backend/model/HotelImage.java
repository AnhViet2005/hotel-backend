package com.hotel.backend.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "hotel_images")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HotelImage {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "hotel_id")
    private Hotel hotel;

    @Column(name = "image_url", length = 500)
    private String imageUrl;

    @Column(name = "is_primary")
    private Boolean isPrimary;
}
