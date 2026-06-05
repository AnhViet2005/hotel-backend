package com.hotel.backend.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.Set;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "full_name")
    private String fullName;

    @Column(unique = true, length = 100)
    private String email;

    @Column(name = "password_hash")
    private String passwordHash;

    @Column(length = 20)
    private String phone;

    @Column(name = "avatar_url", length = 500)
    private String avatarUrl;

    @Column(name = "is_active")
    private Boolean isActive;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "role_id")
    private Role role;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (isActive == null) isActive = true;
    }

    @OneToMany(mappedBy = "owner")
    private Set<Hotel> ownedHotels;

    @OneToMany(mappedBy = "user")
    private Set<Booking> bookings;

    @OneToMany(mappedBy = "user")
    private Set<Review> reviews;

    @OneToMany(mappedBy = "user")
    private Set<Favorite> favorites;
}
