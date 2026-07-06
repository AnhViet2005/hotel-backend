package com.hotel.backend.service;

import com.hotel.backend.dto.AdminHotelRequest;
import com.hotel.backend.dto.AdminHotelResponse;
import com.hotel.backend.model.Hotel;

import com.hotel.backend.model.User;
import com.hotel.backend.repository.HotelRepository;
import com.hotel.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor

public class AdminHotelService {

    private final HotelRepository hotelRepository;
    private final UserRepository userRepository;

    private User getCurrentUser() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Người dùng không tồn tại."));
    }

    private boolean isAdmin(User user) {
        if (user.getRole() == null) return false;
        String role = user.getRole().getRoleName();
        return "ADMIN".equalsIgnoreCase(role) || "HỆ THỐNG".equalsIgnoreCase(role);
    }

    public List<AdminHotelResponse> getAll(String keyword) {
        User current = getCurrentUser();
        List<Hotel> hotels;
        if (keyword != null && !keyword.isBlank()) {
            hotels = hotelRepository.searchHotels(keyword);
        } else {
            hotels = hotelRepository.findAll();
        }
        
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
        if (isAdmin(current)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Admin chỉ có quyền xóa, không có quyền thêm mới khách sạn.");
        }
        Hotel hotel = Hotel.builder()
                .hotelName(request.getName())
                .description(request.getDescription())
                .addressLine(request.getAddressLine())
                .city(request.getCity())
                .district(request.getDistrict())
                .phone(request.getPhone())
                .email(request.getEmail())
                .starRating(request.getRating())
                .basePrice(request.getBasePrice())
                .depositPercentage(request.getDepositPercentage())
                .isActive(true)
                .owner(current)
                .build();
        return toResponse(hotelRepository.save(hotel));
    }

    @Transactional
    public AdminHotelResponse update(Long id, AdminHotelRequest request) {
        User current = getCurrentUser();
        if (isAdmin(current)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Admin chỉ có quyền xóa, không có quyền sửa đổi thông tin khách sạn.");
        }
        Hotel hotel = hotelRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy khách sạn."));
        
        if (!isAdmin(current) && (hotel.getOwner() == null || !hotel.getOwner().getId().equals(current.getId()))) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Không có quyền sửa khách sạn này.");
        }

        hotel.setHotelName(request.getName());
        hotel.setDescription(request.getDescription());
        hotel.setAddressLine(request.getAddressLine());
        hotel.setCity(request.getCity());
        hotel.setDistrict(request.getDistrict());
        hotel.setPhone(request.getPhone());
        hotel.setEmail(request.getEmail());
        hotel.setStarRating(request.getRating());
        hotel.setBasePrice(request.getBasePrice());
        hotel.setDepositPercentage(request.getDepositPercentage());
        
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
        
        if (!isAdmin(current)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Chỉ Quản trị viên hệ thống mới có quyền xóa vĩnh viễn.");
        }

        hotelRepository.delete(hotel);
    }

    @Transactional
    public AdminHotelResponse toggleStatus(Long id) {
        User current = getCurrentUser();
        Hotel hotel = hotelRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy khách sạn."));
        
        if (!isAdmin(current) && (hotel.getOwner() == null || !hotel.getOwner().getId().equals(current.getId()))) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Không có quyền thay đổi trạng thái khách sạn này.");
        }

        hotel.setIsActive(!Boolean.TRUE.equals(hotel.getIsActive()));
        return toResponse(hotelRepository.save(hotel));
    }

    @Transactional
    public AdminHotelResponse approveHotel(Long id) {
        User current = getCurrentUser();
        if (!isAdmin(current)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Chỉ Quản trị viên mới có quyền phê duyệt khách sạn.");
        }
        
        Hotel hotel = hotelRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy khách sạn."));
        
        hotel.setIsApproved(true);
        hotel.setIsActive(true); // Đảm bảo khi duyệt xong thì khách sạn cũng ở trạng thái hoạt động
        
        return toResponse(hotelRepository.save(hotel));
    }

    public AdminHotelResponse toResponse(Hotel hotel) {
        String imageUrl = hotel.getImages() != null && !hotel.getImages().isEmpty()
                ? hotel.getImages().iterator().next().getImageUrl()
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
                .depositPercentage(hotel.getDepositPercentage() != null ? hotel.getDepositPercentage() : 30)
                .isApproved(hotel.getIsApproved())
                .build();
    }
}
