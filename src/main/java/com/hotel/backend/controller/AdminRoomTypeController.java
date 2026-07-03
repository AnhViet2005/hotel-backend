package com.hotel.backend.controller;

import com.hotel.backend.dto.AdminRoomTypeRequest;
import com.hotel.backend.dto.AdminRoomTypeResponse;
import com.hotel.backend.model.*;
import com.hotel.backend.repository.HotelRepository;
import com.hotel.backend.repository.RoomImageRepository;
import com.hotel.backend.repository.RoomTypeRepository;
import com.hotel.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
@SuppressWarnings("null")
public class AdminRoomTypeController {

    private final RoomTypeRepository roomTypeRepository;
    private final HotelRepository hotelRepository;
    private final UserRepository userRepository;
    private final RoomImageRepository roomImageRepository;

    private User getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getPrincipal() == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not authenticated");
        }
        String email = auth.getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found"));
    }

    private boolean isAdmin(User user) {
        if (user.getRole() == null) return false;
        String role = user.getRole().getRoleName();
        return "ADMIN".equalsIgnoreCase(role) || "HỆ THỐNG".equalsIgnoreCase(role);
    }

    @GetMapping("/hotels/{hotelId}/room-types")
    @Transactional(readOnly = true)
    public ResponseEntity<List<AdminRoomTypeResponse>> getRoomTypes(@PathVariable Long hotelId) {
        User current = getCurrentUser();
        Hotel hotel = hotelRepository.findById(hotelId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy khách sạn."));
        if (!isAdmin(current) && (hotel.getOwner() == null || !hotel.getOwner().getId().equals(current.getId()))) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Không có quyền truy cập loại phòng của khách sạn này.");
        }
        List<RoomType> types = roomTypeRepository.findByHotelId(hotelId);
        return ResponseEntity.ok(types.stream().map(this::toResponse).collect(Collectors.toList()));
    }

    @PostMapping("/hotels/{hotelId}/room-types")
    @Transactional
    public ResponseEntity<AdminRoomTypeResponse> createRoomType(@PathVariable Long hotelId, @RequestBody AdminRoomTypeRequest request) {
        User current = getCurrentUser();
        Hotel hotel = hotelRepository.findById(hotelId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy khách sạn."));
        if (isAdmin(current)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Admin chỉ có quyền xóa, không có quyền thêm phòng.");
        }
        if (hotel.getOwner() == null || !hotel.getOwner().getId().equals(current.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Bạn không phải chủ khách sạn này.");
        }
        
        RoomType roomType = RoomType.builder()
                .hotel(hotel)
                .typeName(request.getTypeName())
                .description(request.getDescription())
                .basePrice(request.getBasePrice())
                .maxAdults(request.getMaxAdults())
                .maxChildren(request.getMaxChildren())
                .totalRooms(request.getTotalRooms())
                .roomSize(request.getRoomSize())
                .build();
        
        RoomType saved = roomTypeRepository.save(roomType);
        
        if (request.getImageUrls() != null && !request.getImageUrls().isEmpty()) {
            for (int i = 0; i < Math.min(request.getImageUrls().size(), 4); i++) {
                String url = request.getImageUrls().get(i);
                if (url != null && !url.isBlank()) {
                    RoomImage img = RoomImage.builder()
                            .roomType(saved)
                            .imageUrl(url)
                            .isPrimary(i == 0)
                            .build();
                    roomImageRepository.save(img);
                }
            }
        }
        
        return ResponseEntity.ok(toResponse(saved));
    }

    @PutMapping("/room-types/{id}")
    @Transactional
    public ResponseEntity<AdminRoomTypeResponse> updateRoomType(@PathVariable Long id, @RequestBody AdminRoomTypeRequest request) {
        User current = getCurrentUser();
        RoomType roomType = roomTypeRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy loại phòng."));
        
        Hotel hotel = roomType.getHotel();
        if (hotel == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Loại phòng không liên kết với khách sạn nào.");
        }
        if (isAdmin(current)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Admin chỉ có quyền xóa, không có quyền sửa phòng.");
        }
        if (hotel.getOwner() == null || !hotel.getOwner().getId().equals(current.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Bạn không có quyền cập nhật loại phòng của khách sạn này.");
        }
        
        if (request.getTypeName() != null) roomType.setTypeName(request.getTypeName());
        if (request.getDescription() != null) roomType.setDescription(request.getDescription());
        if (request.getBasePrice() != null) roomType.setBasePrice(request.getBasePrice());
        if (request.getMaxAdults() != null) roomType.setMaxAdults(request.getMaxAdults());
        if (request.getMaxChildren() != null) roomType.setMaxChildren(request.getMaxChildren());
        if (request.getTotalRooms() != null) roomType.setTotalRooms(request.getTotalRooms());
        if (request.getRoomSize() != null) roomType.setRoomSize(request.getRoomSize());

        if (request.getImageUrls() != null) {
            // Xóa hết ảnh cũ của loại phòng này trước khi thêm mới (hoặc đồng bộ)
            List<RoomImage> oldImages = roomImageRepository.findByRoomTypeId(id);
            roomImageRepository.deleteAll(oldImages);
            
            for (int i = 0; i < Math.min(request.getImageUrls().size(), 4); i++) {
                String url = request.getImageUrls().get(i);
                if (url != null && !url.isBlank()) {
                    RoomImage img = RoomImage.builder()
                            .roomType(roomType)
                            .imageUrl(url)
                            .isPrimary(i == 0)
                            .build();
                    roomImageRepository.save(img);
                }
            }
        }

        return ResponseEntity.ok(toResponse(roomTypeRepository.save(roomType)));
    }

    @DeleteMapping("/room-types/{id}")
    public ResponseEntity<Void> deleteRoomType(@PathVariable Long id) {
        User current = getCurrentUser();
        RoomType roomType = roomTypeRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy loại phòng."));
        
        Hotel hotel = roomType.getHotel();
        if (hotel != null) {
            if (hotel.getOwner() == null || !hotel.getOwner().getId().equals(current.getId())) {
                // Cho phép nếu là Admin
                if (!isAdmin(current)) {
                    throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Bạn không có quyền xóa loại phòng của khách sạn này.");
                }
            }
        }
        
        roomTypeRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }
    
    private AdminRoomTypeResponse toResponse(RoomType rt) {
        try {
            System.out.println("Mapping RoomType ID: " + rt.getId() + ", Name: " + rt.getTypeName());
            List<RoomImage> images = roomImageRepository.findByRoomTypeId(rt.getId());
            List<String> imageUrls = images.stream()
                    .sorted((a, b) -> {
                        if (Boolean.TRUE.equals(a.getIsPrimary())) return -1;
                        if (Boolean.TRUE.equals(b.getIsPrimary())) return 1;
                        return 0;
                    })
                    .map(RoomImage::getImageUrl)
                    .collect(Collectors.toList());
                    
            return AdminRoomTypeResponse.builder()
                    .id(rt.getId())
                    .typeName(rt.getTypeName())
                    .description(rt.getDescription())
                    .basePrice(rt.getBasePrice())
                    .maxAdults(rt.getMaxAdults())
                    .maxChildren(rt.getMaxChildren())
                    .totalRooms(rt.getTotalRooms())
                    .roomSize(rt.getRoomSize())
                    .imageUrls(imageUrls)
                    .build();
        } catch (Exception e) {
            System.err.println("Error mapping RoomType ID " + rt.getId() + ": " + e.getMessage());
            return AdminRoomTypeResponse.builder()
                    .id(rt.getId())
                    .typeName(rt.getTypeName() + " (Lỗi load dữ liệu)")
                    .imageUrls(List.of())
                    .build();
        }
    }
}
