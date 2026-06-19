package com.hotel.backend.service;

import com.hotel.backend.dto.PublicHotelResponse;
import com.hotel.backend.dto.PublicRoomTypeResponse;
import com.hotel.backend.model.Hotel;
import com.hotel.backend.model.HotelImage;
import com.hotel.backend.model.RoomType;
import com.hotel.backend.repository.HotelRepository;
import com.hotel.backend.repository.RoomTypeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@SuppressWarnings("null")
public class PublicHotelService {

    private final HotelRepository hotelRepository;
    private final RoomTypeRepository roomTypeRepository;
    private final com.hotel.backend.repository.BookingRoomRepository bookingRoomRepository;
    private final com.hotel.backend.repository.RoomCalendarRepository roomCalendarRepository;

    public List<PublicHotelResponse> getAllActiveHotels(String keyword) {
        List<Hotel> hotels;
        if (keyword != null && !keyword.isEmpty()) {
            hotels = hotelRepository.searchHotels(keyword);
        } else {
            hotels = hotelRepository.findByIsActiveTrue();
        }

        return hotels.stream().map(this::mapToPublicHotelResponse).collect(Collectors.toList());
    }

    public PublicHotelResponse getHotelById(Long id) {
        Hotel hotel = hotelRepository.findById(id).orElseThrow(() -> new RuntimeException("Hotel not found"));
        return mapToPublicHotelResponse(hotel);
    }

    private PublicHotelResponse mapToPublicHotelResponse(Hotel hotel) {
        java.util.Set<HotelImage> images = hotel.getImages() != null ? hotel.getImages() : java.util.Collections.emptySet();
        
        String primaryImage = images.stream()
                .filter(img -> Boolean.TRUE.equals(img.getIsPrimary()))
                .map(HotelImage::getImageUrl)
                .findFirst()
                .orElse(images.isEmpty() ? null : images.iterator().next().getImageUrl());

        List<String> gallery = images.stream()
                .map(HotelImage::getImageUrl)
                .collect(Collectors.toList());

        List<PublicRoomTypeResponse> roomTypes = roomTypeRepository.findByHotelId(hotel.getId()).stream()
                .map(this::mapToPublicRoomTypeResponse)
                .collect(Collectors.toList());

        BigDecimal minPrice = hotel.getBasePrice();
        if (minPrice == null || minPrice.compareTo(BigDecimal.ZERO) <= 0) {
            minPrice = roomTypes.stream()
                    .map(PublicRoomTypeResponse::getPrice)
                    .min(BigDecimal::compareTo)
                    .orElse(BigDecimal.ZERO);
        }

        List<String> amenities = hotel.getHotelAmenities() != null 
                ? hotel.getHotelAmenities().stream()
                    .map(ha -> ha.getAmenity().getAmenityName())
                    .collect(Collectors.toList())
                : List.of("Free WiFi", "Parking");

        int reviewsCount = hotel.getReviews() != null ? hotel.getReviews().size() : 0;

        return PublicHotelResponse.builder()
                .id(hotel.getId())
                .name(hotel.getHotelName())
                .location(hotel.getCity())
                .address(hotel.getAddressLine() + (hotel.getDistrict() != null ? ", " + hotel.getDistrict() : ""))
                .price(minPrice)
                .rating(hotel.getStarRating() != null ? hotel.getStarRating() : BigDecimal.valueOf(4.5))
                .reviews(reviewsCount > 0 ? reviewsCount : 120) // Use real count if available
                .image(primaryImage)
                .gallery(gallery)
                .description(hotel.getDescription())
                .amenities(amenities)
                .rooms(roomTypes)
                .build();
    }

    private PublicRoomTypeResponse mapToPublicRoomTypeResponse(RoomType rt) {
        String roomImage = rt.getImages() != null && !rt.getImages().isEmpty()
                ? rt.getImages().iterator().next().getImageUrl()
                : null;

        int totalRooms = rt.getTotalRooms() != null ? rt.getTotalRooms() : 10;
        int activeBookings = 0;
        try {
            List<com.hotel.backend.model.BookingRoom> bookingRooms = bookingRoomRepository.findByRoomTypeId(rt.getId());
            for (com.hotel.backend.model.BookingRoom br : bookingRooms) {
                com.hotel.backend.model.Booking b = br.getBooking();
                if (b != null && b.getStatus() != com.hotel.backend.model.Booking.BookingStatus.CANCELLED) {
                    if (b.getCheckOut() != null && b.getCheckOut().isAfter(java.time.LocalDate.now())) {
                        activeBookings += br.getQuantity() != null ? br.getQuantity() : 1;
                    }
                }
            }
        } catch (Exception e) {
            // Ignore
        }
        int availableRooms = Math.max(0, totalRooms - activeBookings);

        String nextAvailable = null;
        if (availableRooms == 0) {
            nextAvailable = findNextAvailableDate(rt);
        }

        List<String> imageUrls = rt.getImages() != null ? rt.getImages().stream()
                .sorted((a, b) -> {
                    if (Boolean.TRUE.equals(a.getIsPrimary())) return -1;
                    if (Boolean.TRUE.equals(b.getIsPrimary())) return 1;
                    return 0;
                })
                .map(com.hotel.backend.model.RoomImage::getImageUrl)
                .collect(Collectors.toList()) : List.of();

        return PublicRoomTypeResponse.builder()
                .id(rt.getId())
                .name(rt.getTypeName())
                .capacity(rt.getMaxAdults() != null ? rt.getMaxAdults() : 2)
                .size(rt.getRoomSize() != null ? rt.getRoomSize() + " m2" : "45 m2")
                .bedType("King Bed")
                .price(rt.getBasePrice())
                .features(List.of("City View", "Private Balcony"))
                .image(roomImage)
                .imageUrls(imageUrls)
                .description(rt.getDescription())
                .availableRooms(availableRooms)
                .nextAvailableDate(nextAvailable)
                .build();
    }

    private String findNextAvailableDate(RoomType rt) {
        java.time.LocalDate today = java.time.LocalDate.now();
        // Check RoomCalendar first
        List<com.hotel.backend.model.RoomCalendar> calendars = 
            roomCalendarRepository.findByRoomTypeIdAndDateGreaterThanEqualOrderByDateAsc(rt.getId(), today);
        
        java.util.Map<java.time.LocalDate, com.hotel.backend.model.RoomCalendar> calMap = calendars.stream()
            .collect(Collectors.toMap(com.hotel.backend.model.RoomCalendar::getDate, c -> c));

        // Scan next 30 days
        for (int i = 0; i < 30; i++) {
            java.time.LocalDate date = today.plusDays(i);
            com.hotel.backend.model.RoomCalendar cal = calMap.get(date);
            
            int total = rt.getTotalRooms() != null ? rt.getTotalRooms() : 10;
            int booked = 0;

            if (cal != null) {
                if (Boolean.FALSE.equals(cal.getIsAvailable())) continue;
                total = cal.getTotalRooms() != null ? cal.getTotalRooms() : total;
                booked = cal.getBookedRooms() != null ? cal.getBookedRooms() : 0;
            } else {
                // If not in calendar, check active bookings manually for this specific date
                try {
                    List<com.hotel.backend.model.BookingRoom> brs = bookingRoomRepository.findByRoomTypeId(rt.getId());
                    for (com.hotel.backend.model.BookingRoom br : brs) {
                        com.hotel.backend.model.Booking b = br.getBooking();
                        if (b != null && b.getStatus() != com.hotel.backend.model.Booking.BookingStatus.CANCELLED) {
                            if (!date.isBefore(b.getCheckIn()) && date.isBefore(b.getCheckOut())) {
                                booked += br.getQuantity() != null ? br.getQuantity() : 1;
                            }
                        }
                    }
                } catch (Exception ignored) {}
            }

            if (booked < total) {
                return date.toString();
            }
        }
        return "Liên hệ trực tiếp";
    }
}

