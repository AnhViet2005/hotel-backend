package com.hotel.backend.model;

import jakarta.persistence.*;
import lombok.*;
import java.util.Set;

@Entity
@Table(name = "amenities")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Amenity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "amenity_name")
    private String amenityName;

    @Column(name = "icon_url")
    private String iconUrl;

    @OneToMany(mappedBy = "amenity")
    private Set<HotelAmenity> hotelAmenities;
}
