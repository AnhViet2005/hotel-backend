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

public class PublicHotelService {

    private final HotelRepository hotelRepository;
    private final RoomTypeRepository roomTypeRepository;
    private final com.hotel.backend.repository.BookingRoomRepository bookingRoomRepository;
    private final com.hotel.backend.repository.RoomCalendarRepository roomCalendarRepository;

    public List<PublicHotelResponse> getAllActiveHotels(String keyword, String checkIn, String checkOut) {
        List<Hotel> hotels;
        if (keyword != null && !keyword.isEmpty()) {
            hotels = hotelRepository.searchHotels(keyword);
        } else {
            hotels = hotelRepository.findByIsActiveTrueAndIsApprovedTrue();
        }

        return hotels.stream().map(h -> mapToPublicHotelResponse(h, checkIn, checkOut)).collect(Collectors.toList());
    }

    public PublicHotelResponse getHotelById(Long id, String checkIn, String checkOut) {
        Hotel hotel = hotelRepository.findById(id).orElseThrow(() -> new RuntimeException("Hotel not found"));
        return mapToPublicHotelResponse(hotel, checkIn, checkOut);
    }

    public PublicHotelResponse getHotelByIdSimple(Long id) {
        Hotel hotel = hotelRepository.findByIdWithImages(id)
                .orElseThrow(() -> new RuntimeException("Hotel not found"));
        return mapToPublicHotelResponseSimple(hotel);
    }

    public PublicHotelResponse mapToPublicHotelResponseSimple(Hotel hotel) {
        java.util.Set<HotelImage> images = hotel.getImages() != null ? hotel.getImages() : java.util.Collections.emptySet();
        
        String primaryImage = images.stream()
                .filter(img -> Boolean.TRUE.equals(img.getIsPrimary()))
                .map(HotelImage::getImageUrl)
                .findFirst()
                .orElse(images.isEmpty() ? null : images.iterator().next().getImageUrl());

        BigDecimal minPrice = hotel.getBasePrice();
        if (minPrice == null || minPrice.compareTo(BigDecimal.ZERO) <= 0) {
            minPrice = BigDecimal.ZERO;
        }

        int reviewsCount = hotel.getReviews() != null ? hotel.getReviews().size() : 0;

        return PublicHotelResponse.builder()
                .id(hotel.getId())
                .name(hotel.getHotelName())
                .location(hotel.getCity())
                .address(hotel.getAddressLine() + (hotel.getDistrict() != null ? ", " + hotel.getDistrict() : ""))
                .price(minPrice)
                .rating(hotel.getStarRating() != null ? hotel.getStarRating() : BigDecimal.valueOf(4.5))
                .reviews(reviewsCount > 0 ? reviewsCount : 120)
                .image(primaryImage)
                .description(hotel.getDescription())
                .build();
    }

    public PublicHotelResponse mapToPublicHotelResponse(Hotel hotel, String checkIn, String checkOut) {
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
                .map(rt -> mapToPublicRoomTypeResponse(rt, checkIn, checkOut))
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
                .ownerId(hotel.getOwner() != null ? hotel.getOwner().getId() : null)
                .ownerName(hotel.getOwner() != null ? hotel.getOwner().getFullName() : "Admin")
                .build();
    }

    private PublicRoomTypeResponse mapToPublicRoomTypeResponse(RoomType rt, String checkInStr, String checkOutStr) {
        String roomImage = rt.getImages() != null && !rt.getImages().isEmpty()
                ? rt.getImages().iterator().next().getImageUrl()
                : null;

        java.time.LocalDate checkIn = (checkInStr != null && !checkInStr.isEmpty()) ? java.time.LocalDate.parse(checkInStr) : java.time.LocalDate.now();
        java.time.LocalDate checkOut = (checkOutStr != null && !checkOutStr.isEmpty()) ? java.time.LocalDate.parse(checkOutStr) : checkIn.plusDays(1);

        int totalRooms = rt.getTotalRooms() != null ? rt.getTotalRooms() : 10;
        int maxBookedInPeriod = 0;

        // Check availability strictly for the requested period
        for (java.time.LocalDate date = checkIn; date.isBefore(checkOut); date = date.plusDays(1)) {
            int bookedOnDate = 0;
            // 1. Check override in RoomCalendar
            java.util.Optional<com.hotel.backend.model.RoomCalendar> calOpt = 
                roomCalendarRepository.findByRoomTypeIdAndDate(rt.getId(), date);
            
            if (calOpt.isPresent()) {
                com.hotel.backend.model.RoomCalendar cal = calOpt.get();
                if (Boolean.FALSE.equals(cal.getIsAvailable())) {
                    bookedOnDate = totalRooms; // Consider full if closed
                } else {
                    bookedOnDate = cal.getBookedRooms() != null ? cal.getBookedRooms() : 0;
                }
            } else {
                // 2. Fallback to manual booking check
                try {
                    List<com.hotel.backend.model.BookingRoom> brs = bookingRoomRepository.findByRoomTypeId(rt.getId());
                    for (com.hotel.backend.model.BookingRoom br : brs) {
                        com.hotel.backend.model.Booking b = br.getBooking();
                        if (b != null && b.getStatus() != com.hotel.backend.model.Booking.BookingStatus.CANCELLED) {
                            if (!date.isBefore(b.getCheckIn()) && date.isBefore(b.getCheckOut())) {
                                bookedOnDate += br.getQuantity() != null ? br.getQuantity() : 1;
                            }
                        }
                    }
                } catch (Exception ignored) {}
            }
            maxBookedInPeriod = Math.max(maxBookedInPeriod, bookedOnDate);
        }

        int availableRooms = Math.max(0, totalRooms - maxBookedInPeriod);

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

