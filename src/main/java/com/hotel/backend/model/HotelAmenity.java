package com.hotel.backend.model;

import jakarta.persistence.*;
import lombok.*;
import java.io.Serializable;
import java.math.BigDecimal;

@Entity
@Table(name = "hotel_amenities")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HotelAmenity {

    @EmbeddedId
    private HotelAmenityId id;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("hotelId")
    @JoinColumn(name = "hotel_id")
    private Hotel hotel;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("amenityId")
    @JoinColumn(name = "amenity_id")
    private Amenity amenity;

    @Column(name = "is_free")
    private Boolean isFree;

    @Column(name = "additional_fee", precision = 10, scale = 2)
    private BigDecimal additionalFee;

    @Embeddable
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @EqualsAndHashCode
    @Builder
    public static class HotelAmenityId implements Serializable {
        @Column(name = "hotel_id")
        private Long hotelId;

        @Column(name = "amenity_id")
        private Long amenityId;
    }
}
