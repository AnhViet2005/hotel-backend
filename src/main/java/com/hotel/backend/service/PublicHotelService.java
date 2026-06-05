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
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PublicHotelService {

    private final HotelRepository hotelRepository;
    private final RoomTypeRepository roomTypeRepository;

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

        return PublicRoomTypeResponse.builder()
                .id(rt.getId())
                .name(rt.getTypeName())
                .capacity(rt.getMaxAdults() != null ? rt.getMaxAdults() : 2)
                .size(rt.getRoomSize() != null ? rt.getRoomSize() + " m2" : "45 m2")
                .bedType("King Bed") // Mock bed type as it's not in the model
                .price(rt.getBasePrice())
                .features(List.of("City View", "Private Balcony")) // Mock features
                .image(roomImage)
                .build();
    }
}

