package com.hotel.backend.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalTime;

@Entity
@Table(name = "hotel_policies")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HotelPolicy {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "hotel_id", unique = true)
    private Hotel hotel;

    @Column(name = "check_in_time")
    private LocalTime checkInTime;

    @Column(name = "check_out_time")
    private LocalTime checkOutTime;

    @Column(name = "cancellation_policy", columnDefinition = "TEXT")
    private String cancellationPolicy;

    @Column(name = "children_policy", columnDefinition = "TEXT")
    private String childrenPolicy;

    @Column(name = "pet_policy", columnDefinition = "TEXT")
    private String petPolicy;
}
