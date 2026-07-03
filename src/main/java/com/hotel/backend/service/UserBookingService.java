package com.hotel.backend.service;

import com.hotel.backend.dto.BookingRequest;
import com.hotel.backend.dto.UserBookingResponse;
import com.hotel.backend.model.Booking;
import com.hotel.backend.model.BookingRoom;
import com.hotel.backend.model.Hotel;
import com.hotel.backend.model.Payment;
import com.hotel.backend.model.RoomType;
import com.hotel.backend.model.User;
import com.hotel.backend.repository.BookingRepository;
import com.hotel.backend.repository.BookingRoomRepository;
import com.hotel.backend.repository.HotelRepository;
import com.hotel.backend.repository.RoomTypeRepository;
import com.hotel.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@SuppressWarnings("null")
public class UserBookingService {

    private final BookingRepository bookingRepository;
    private final HotelRepository hotelRepository;
    private final UserRepository userRepository;
    private final RoomTypeRepository roomTypeRepository;
    private final BookingRoomRepository bookingRoomRepository;
    private final NotificationService notificationService;
    private final EmailService emailService;

    // ──────────────────────────────────────────────
    // Helper: get current authenticated user
    // ──────────────────────────────────────────────
    private User getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        return userRepository.findByEmail(auth.getName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found"));
    }

    // ──────────────────────────────────────────────
    // Create Booking (PENDING → 30% cọc qua VNPay)
    // ──────────────────────────────────────────────
    @Transactional
    public Long createBooking(BookingRequest request, String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        Hotel hotel = hotelRepository.findById(request.getHotelId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Hotel not found"));

        long nights = ChronoUnit.DAYS.between(request.getCheckIn(), request.getCheckOut());
        if (nights <= 0) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Check-out phải sau Check-in");

        BigDecimal roomTotal = BigDecimal.ZERO;
        java.util.List<com.hotel.backend.dto.RoomSelection> selections = request.getRooms();

        // If no rooms list provided, fall back to legacy single room logic
        if (selections == null || selections.isEmpty()) {
            if (request.getRoomTypeId() == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No rooms selected");
            }
            selections = java.util.List.of(new com.hotel.backend.dto.RoomSelection(request.getRoomTypeId(), request.getQuantity() != null ? request.getQuantity() : 1));
        }

        // Create the booking record first (we'll update total later or calculate first)
        Booking booking = Booking.builder()
                .bookingCode(generateBookingCode())
                .user(user)
                .hotel(hotel)
                .guestName(request.getGuestName())
                .guestEmail(request.getGuestEmail())
                .guestPhone(request.getGuestPhone())
                .checkIn(request.getCheckIn())
                .checkOut(request.getCheckOut())
                .status(Booking.BookingStatus.PENDING)
                .remainingPaymentStatus(Booking.RemainingPaymentStatus.UNPAID)
                .build();

        Booking saved = bookingRepository.save(booking);

        // Process each selected room
        for (com.hotel.backend.dto.RoomSelection selection : selections) {
            RoomType rt = roomTypeRepository.findById(selection.getRoomTypeId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Room type not found: " + selection.getRoomTypeId()));

            BigDecimal itemPrice = rt.getBasePrice();
            BigDecimal itemTotal = itemPrice
                    .multiply(BigDecimal.valueOf(selection.getQuantity()))
                    .multiply(BigDecimal.valueOf(nights));
            
            roomTotal = roomTotal.add(itemTotal);

            bookingRoomRepository.save(BookingRoom.builder()
                    .booking(saved)
                    .roomType(rt)
                    .quantity(selection.getQuantity())
                    .pricePerNight(itemPrice)
                    .build());
        }
        
        // Cộng thêm thuế & phí dịch vụ (100,000 VND)
        BigDecimal taxesAndFees = new BigDecimal("100000");
        BigDecimal total = roomTotal.add(taxesAndFees);

        // Calculate revenue split based on hotel configuration (default 30/70)
        double depositPercent = (hotel.getDepositPercentage() != null ? hotel.getDepositPercentage() : 30) / 100.0;
        BigDecimal adminRevenue = total.multiply(BigDecimal.valueOf(depositPercent));
        BigDecimal hotelOwnerRevenue = total.subtract(adminRevenue);

        saved.setSubtotal(total);
        saved.setTotalAmount(total);
        saved.setAdminRevenue(adminRevenue);
        saved.setHotelOwnerRevenue(hotelOwnerRevenue);
        
        bookingRepository.save(saved);
        
        // Thông báo cho khách hàng: Bạn đã gửi đơn đặt phòng thành công
        notificationService.notifyBookingCreated(saved);

        return saved.getId();
    }

    // ──────────────────────────────────────────────
    // Confirm Booking (sau VNPay callback → CONFIRMED)
    // ──────────────────────────────────────────────
    @Transactional
    public void confirmBooking(Long bookingId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Booking not found"));
        if (booking.getStatus() != Booking.BookingStatus.PENDING) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Only PENDING bookings can be confirmed");
        }
        booking.setStatus(Booking.BookingStatus.CONFIRMED);
        Booking saved = bookingRepository.save(booking);

        // Gửi thông báo cho khách hàng khi đơn đã được xác nhận (nhận cọc 30%)
        notificationService.notifyBookingConfirmed(saved);

        // Gửi thông báo cho chủ khách sạn khi nhận cọc 30%
        if (saved.getHotel() != null) {
            Long ownerId = saved.getHotel().getOwner() != null ? saved.getHotel().getOwner().getId() : null;
            notificationService.notifyDepositReceived(
                saved.getId(),
                saved.getBookingCode(),
                saved.getAdminRevenue(),
                ownerId
            );
        }

        // Gửi email xác nhận đặt phòng thật
        emailService.sendBookingConfirmationEmail(saved);
    }

    // ──────────────────────────────────────────────
    // Pay Remaining (70% do người dùng thanh toán)
    // ──────────────────────────────────────────────
    @Transactional
    public UserBookingResponse payRemaining(Long bookingId, String method) {
        User current = getCurrentUser();

        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Booking not found"));

        // Chỉ người đặt phòng mới được thanh toán
        if (!booking.getUser().getId().equals(current.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Không có quyền thanh toán đơn này");
        }

        // Chỉ thanh toán khi CONFIRMED (admin đã xác nhận nhận cọc)
        if (booking.getStatus() != Booking.BookingStatus.CONFIRMED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Chỉ có thể thanh toán phần còn lại khi đơn đã được xác nhận (CONFIRMED)");
        }

        // Đã thanh toán rồi?
        if (booking.getRemainingPaymentStatus() == Booking.RemainingPaymentStatus.PAID) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Phần còn lại đã được thanh toán");
        }

        // Validate phương thức
        Payment.PaymentMethod payMethod;
        if (method == null || method.trim().isEmpty()) {
            payMethod = Payment.PaymentMethod.CASH;
        } else {
            try {
                payMethod = Payment.PaymentMethod.valueOf(method.toUpperCase().trim());
            } catch (IllegalArgumentException e) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Phương thức thanh toán không hợp lệ: " + method);
            }
        }

        // Cập nhật booking
        booking.setRemainingPaymentStatus(Booking.RemainingPaymentStatus.PAID);
        booking.setRemainingPaymentMethod(payMethod);
        booking.setRemainingPaidAt(LocalDateTime.now());
        bookingRepository.save(booking);

        // Gửi thông báo đến chủ khách sạn cho admin
        if (booking.getHotel() != null) {
            Long ownerId = booking.getHotel().getOwner() != null ? booking.getHotel().getOwner().getId() : null;
            notificationService.notifyFullPaymentReceived(
                booking.getId(),
                booking.getBookingCode(),
                booking.getTotalAmount(),
                ownerId
            );
        }

        return toUserResponse(booking);
    }

    @Transactional
    public UserBookingResponse cancelBooking(Long bookingId) {
        User current = getCurrentUser();
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Booking not found"));

        if (!booking.getUser().getId().equals(current.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Không có quyền hủy đơn này");
        }

        if (booking.getStatus() == Booking.BookingStatus.COMPLETED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Không thể hủy đơn đã hoàn tất");
        }

        if (booking.getStatus() == Booking.BookingStatus.CANCELLED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Đơn đã được hủy trước đó");
        }

        booking.setStatus(Booking.BookingStatus.CANCELLED);
        booking.setAdminRevenue(BigDecimal.ZERO);
        booking.setHotelOwnerRevenue(BigDecimal.ZERO);
        bookingRepository.save(booking);

        return toUserResponse(booking);
    }

    // ──────────────────────────────────────────────
    // toUserResponse
    // ──────────────────────────────────────────────
    public UserBookingResponse toUserResponse(Booking b) {
        String hotelName = "N/A";
        String hotelImage = null;
        String hotelCity = null;
        String roomTypeName = null;

        Long hotelId = null;
        try {
            if (b.getHotel() != null) {
                hotelId = b.getHotel().getId();
                hotelName = b.getHotel().getHotelName();
                hotelCity = b.getHotel().getCity();
                var images = b.getHotel().getImages();
                if (images != null && !images.isEmpty()) {
                    hotelImage = images.iterator().next().getImageUrl();
                }
            }
        } catch (Exception ignored) {}

        try {
            if (b.getBookingRooms() != null && !b.getBookingRooms().isEmpty()) {
                roomTypeName = b.getBookingRooms().stream()
                        .findFirst()
                        .map(br -> br.getRoomType() != null ? br.getRoomType().getTypeName() : null)
                        .orElse(null);
            }
        } catch (Exception ignored) {}

        BigDecimal total = b.getTotalAmount() != null ? b.getTotalAmount() : BigDecimal.ZERO;
        BigDecimal deposit = b.getAdminRevenue() != null ? b.getAdminRevenue() : total.multiply(new BigDecimal("0.3"));
        BigDecimal remaining = b.getHotelOwnerRevenue() != null ? b.getHotelOwnerRevenue() : total.subtract(deposit);

        return UserBookingResponse.builder()
                .id(b.getId())
                .bookingCode(b.getBookingCode())
                .hotelId(hotelId)
                .hotelName(hotelName)
                .hotelImage(hotelImage)
                .hotelCity(hotelCity)
                .roomTypeName(roomTypeName)
                .guestName(b.getGuestName())
                .guestPhone(b.getGuestPhone())
                .checkIn(b.getCheckIn())
                .checkOut(b.getCheckOut())
                .totalAmount(total)
                .depositAmount(deposit)
                .remainingAmount(remaining)
                .depositPercentage(b.getHotel() != null && b.getHotel().getDepositPercentage() != null ? b.getHotel().getDepositPercentage() : 30)
                .status(b.getStatus() != null ? b.getStatus().name() : "UNKNOWN")
                .remainingPaymentStatus(b.getRemainingPaymentStatus() != null
                        ? b.getRemainingPaymentStatus().name() : "UNPAID")
                .remainingPaymentMethod(b.getRemainingPaymentMethod() != null
                        ? b.getRemainingPaymentMethod().name() : null)
                .remainingPaidAt(b.getRemainingPaidAt())
                .createdAt(b.getCreatedAt())
                .hasReview(b.getReview() != null)
                .reviewRating(b.getReview() != null && b.getReview().getRating() != null ? b.getReview().getRating().intValue() : null)
                .reviewComment(b.getReview() != null ? b.getReview().getComment() : null)
                .build();
    }

    private String generateBookingCode() {
        return "BK-" + UUID.randomUUID().toString().replaceAll("-", "").substring(0, 12).toUpperCase();
    }
}
