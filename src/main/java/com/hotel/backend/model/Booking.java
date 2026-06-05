package com.hotel.backend.model;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Set;

@Entity
@Table(name = "bookings")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Booking {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "booking_code", unique = true, length = 50)
    private String bookingCode;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "hotel_id")
    private Hotel hotel;

    @Column(name = "guest_name")
    private String guestName;

    @Column(name = "guest_email")
    private String guestEmail;

    @Column(name = "guest_phone", length = 20)
    private String guestPhone;

    @Column(name = "check_in")
    private LocalDate checkIn;

    @Column(name = "check_out")
    private LocalDate checkOut;

    @Column(precision = 12, scale = 2)
    private BigDecimal subtotal;

    @Column(name = "discount_amount", precision = 12, scale = 2)
    private BigDecimal discountAmount;

    @Column(name = "total_amount", precision = 12, scale = 2)
    private BigDecimal totalAmount;

    // Revenue split: Admin 30%, Hotel Owner 70%
    @Column(name = "admin_revenue", precision = 12, scale = 2)
    private BigDecimal adminRevenue; // 30% of totalAmount

    @Column(name = "hotel_owner_revenue", precision = 12, scale = 2)
    private BigDecimal hotelOwnerRevenue; // 70% of totalAmount

    @Enumerated(EnumType.STRING)
    private BookingStatus status;

    // 70% remaining payment tracking
    @Enumerated(EnumType.STRING)
    @Column(name = "remaining_payment_status")
    private RemainingPaymentStatus remainingPaymentStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "remaining_payment_method")
    private Payment.PaymentMethod remainingPaymentMethod;

    @Column(name = "remaining_paid_at")
    private LocalDateTime remainingPaidAt;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    @OneToMany(mappedBy = "booking", cascade = CascadeType.ALL)
    private Set<BookingRoom> bookingRooms;

    @OneToOne(mappedBy = "booking", cascade = CascadeType.ALL)
    private Payment payment;

    @OneToOne(mappedBy = "booking", cascade = CascadeType.ALL)
    private Review review;

    public enum BookingStatus {
        PENDING, CONFIRMED, CANCELLED, COMPLETED
    }

    public enum RemainingPaymentStatus {
        UNPAID, PAID
    }
}
