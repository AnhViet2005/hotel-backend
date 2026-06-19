package com.hotel.backend.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "room_images")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RoomImage {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "room_type_id")
    @com.fasterxml.jackson.annotation.JsonIgnore
    private RoomType roomType;

    @Column(name = "image_url", length = 500)
    private String imageUrl;

    @Column(name = "is_primary")
    private Boolean isPrimary;
}
