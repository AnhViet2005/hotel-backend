package com.hotel.backend.model;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "reviews")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Review {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "hotel_id")
    private Hotel hotel;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "booking_id", unique = true)
    private Booking booking;

    @Column(precision = 2, scale = 1)
    private BigDecimal rating;

    @Column(columnDefinition = "TEXT")
    private String comment;

    @Column(name = "is_published")
    private Boolean isPublished;

    @Column(name = "admin_reply", columnDefinition = "TEXT")
    private String adminReply;

    @Column(name = "admin_replied_at")
    private LocalDateTime adminRepliedAt;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (isPublished == null) isPublished = true;
    }
}
