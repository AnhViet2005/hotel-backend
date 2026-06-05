package com.hotel.backend.service;

import com.hotel.backend.dto.AdminHotelRequest;
import com.hotel.backend.dto.AdminHotelResponse;
import com.hotel.backend.model.Hotel;
import com.hotel.backend.model.HotelImage;
import com.hotel.backend.model.User;
import com.hotel.backend.repository.HotelImageRepository;
import com.hotel.backend.repository.HotelRepository;
import com.hotel.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminHotelService {

    private final HotelRepository hotelRepository;
    private final HotelImageRepository hotelImageRepository;
    private final UserRepository userRepository;

    // Helper to get current authenticated user
    private User getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getPrincipal() == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not authenticated");
        }
        String email = auth.getName(); // assuming username is email
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found"));
    }

    // Helper to check if user is admin
    private boolean isAdmin(User user) {
        return user.getRole() != null && "ADMIN".equalsIgnoreCase(user.getRole().getRoleName());
    }

    public List<AdminHotelResponse> getAll(String keyword) {
        User current = getCurrentUser();
        List<Hotel> hotels;
        if (keyword != null && !keyword.isBlank()) {
            hotels = hotelRepository.searchHotels(keyword);
        } else {
            hotels = hotelRepository.findAll();
        }
        // If not admin, filter to hotels owned by the user
        if (!isAdmin(current)) {
            hotels = hotels.stream()
                    .filter(h -> h.getOwner() != null && h.getOwner().getId().equals(current.getId()))
                    .collect(Collectors.toList());
        }
        return hotels.stream().map(this::toResponse).collect(Collectors.toList());
    }

    public AdminHotelResponse getById(Long id) {
        User current = getCurrentUser();
        Hotel hotel = hotelRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy khách sạn."));
        if (!isAdmin(current) && (hotel.getOwner() == null || !hotel.getOwner().getId().equals(current.getId()))) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Không có quyền truy cập khách sạn này.");
        }
        return toResponse(hotel);
    }

    @Transactional
    public AdminHotelResponse create(AdminHotelRequest request) {
        User current = getCurrentUser();
        // Only admin can create any hotel; owners can create only their own
        Hotel hotel = Hotel.builder()
                .hotelName(request.getName())
                .description(request.getDescription())
                .addressLine(request.getAddressLine())
                .city(request.getCity())
                .district(request.getDistrict())
                .ward(request.getWard())
                .starRating(request.getRating())
                .phone(request.getPhone())
                .email(request.getEmail())
                .isActive(request.getIsActive() != null ? request.getIsActive() : true)
                .basePrice(request.getBasePrice())
                .build();
        // Set owner
        if (isAdmin(current)) {
            // If admin provides ownerId in request (optional), you could set here. For now, keep null.
        } else {
            hotel.setOwner(current);
        }
        hotel = hotelRepository.save(hotel);
        if (request.getImageUrl() != null && !request.getImageUrl().isBlank()) {
            HotelImage img = HotelImage.builder()
                    .hotel(hotel)
                    .imageUrl(request.getImageUrl())
                    .isPrimary(true)
                    .build();
            hotelImageRepository.save(img);
        }
        return toResponse(hotel);
    }

    @Transactional
    public AdminHotelResponse update(Long id, AdminHotelRequest request) {
        User current = getCurrentUser();
        Hotel hotel = hotelRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy khách sạn."));
        if (!isAdmin(current) && (hotel.getOwner() == null || !hotel.getOwner().getId().equals(current.getId()))) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Không có quyền cập nhật khách sạn này.");
        }
        // Apply updates (same as before)
        if (request.getName() != null) hotel.setHotelName(request.getName());
        if (request.getDescription() != null) hotel.setDescription(request.getDescription());
        if (request.getAddressLine() != null) hotel.setAddressLine(request.getAddressLine());
        if (request.getCity() != null) hotel.setCity(request.getCity());
        if (request.getDistrict() != null) hotel.setDistrict(request.getDistrict());
        if (request.getWard() != null) hotel.setWard(request.getWard());
        if (request.getRating() != null) hotel.setStarRating(request.getRating());
        if (request.getPhone() != null) hotel.setPhone(request.getPhone());
        if (request.getEmail() != null) hotel.setEmail(request.getEmail());
        if (request.getIsActive() != null) hotel.setIsActive(request.getIsActive());
        if (request.getBasePrice() != null) hotel.setBasePrice(request.getBasePrice());
        if (request.getImageUrl() != null && !request.getImageUrl().isBlank()) {
            hotelImageRepository.findPrimaryByHotelId(id).ifPresentOrElse(
                    img -> { img.setImageUrl(request.getImageUrl()); hotelImageRepository.save(img); },
                    () -> hotelImageRepository.save(HotelImage.builder().hotel(hotel).imageUrl(request.getImageUrl()).isPrimary(true).build())
            );
        }
        return toResponse(hotelRepository.save(hotel));
    }

    @Transactional
    public void delete(Long id) {
        User current = getCurrentUser();
        Hotel hotel = hotelRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy khách sạn."));
        if (!isAdmin(current) && (hotel.getOwner() == null || !hotel.getOwner().getId().equals(current.getId()))) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Không có quyền xóa khách sạn này.");
        }
        hotel.setIsActive(false);
        hotelRepository.save(hotel);
    }

    @Transactional
    public void hardDelete(Long id) {
        User current = getCurrentUser();
        Hotel hotel = hotelRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy khách sạn."));
        if (!isAdmin(current) && (hotel.getOwner() == null || !hotel.getOwner().getId().equals(current.getId()))) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Không có quyền xóa vĩnh viễn khách sạn này.");
        }
        hotelRepository.deleteById(id);
    }

    private AdminHotelResponse toResponse(Hotel hotel) {
        String imageUrl = hotel.getImages() != null
                ? hotel.getImages().stream()
                        .filter(img -> Boolean.TRUE.equals(img.getIsPrimary()))
                        .map(HotelImage::getImageUrl)
                        .findFirst()
                        .orElse(null)
                : null;
        int totalRooms = hotel.getRoomTypes() != null
                ? hotel.getRoomTypes().stream().mapToInt(rt -> rt.getTotalRooms() != null ? rt.getTotalRooms() : 0).sum()
                : 0;
        BigDecimal minPrice = hotel.getBasePrice();
        if (minPrice == null || minPrice.compareTo(BigDecimal.ZERO) <= 0) {
            minPrice = hotel.getRoomTypes() != null
                    ? hotel.getRoomTypes().stream()
                            .map(rt -> rt.getBasePrice())
                            .filter(p -> p != null)
                            .min(BigDecimal::compareTo)
                            .orElse(BigDecimal.ZERO)
                    : BigDecimal.ZERO;
        }
        return AdminHotelResponse.builder()
                .id(hotel.getId())
                .name(hotel.getHotelName())
                .location((hotel.getCity() != null ? hotel.getCity() : "") +
                        (hotel.getDistrict() != null ? ", " + hotel.getDistrict() : ""))
                .rating(hotel.getStarRating())
                .price(minPrice.longValue() > 0
                        ? String.format("%,.0f", minPrice.doubleValue()).replace(",", ".") + "đ"
                        : "—")
                .basePrice(hotel.getBasePrice())
                .status(Boolean.TRUE.equals(hotel.getIsActive()) ? "active" : "inactive")
                .rooms(totalRooms)
                .image(imageUrl)
                .description(hotel.getDescription())
                .addressLine(hotel.getAddressLine())
                .phone(hotel.getPhone())
                .email(hotel.getEmail())
                .build();
    }
}
