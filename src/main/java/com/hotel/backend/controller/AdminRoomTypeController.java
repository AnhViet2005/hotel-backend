package com.hotel.backend.controller;

import com.hotel.backend.model.Hotel;
import com.hotel.backend.model.RoomType;
import com.hotel.backend.repository.HotelRepository;
import com.hotel.backend.repository.RoomTypeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import com.hotel.backend.model.User;
import com.hotel.backend.repository.UserRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class AdminRoomTypeController {

    private final RoomTypeRepository roomTypeRepository;
    private final HotelRepository hotelRepository;
    private final UserRepository userRepository;

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
        return user.getRole() != null && "ADMIN".equalsIgnoreCase(user.getRole().getRoleName());
    }

    @GetMapping("/hotels/{hotelId}/room-types")
    public ResponseEntity<List<RoomType>> getRoomTypes(@PathVariable Long hotelId) {
        User current = getCurrentUser();
        Hotel hotel = hotelRepository.findById(hotelId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy khách sạn."));
        if (!isAdmin(current) && (hotel.getOwner() == null || !hotel.getOwner().getId().equals(current.getId()))) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Không có quyền truy cập loại phòng của khách sạn này.");
        }
        return ResponseEntity.ok(roomTypeRepository.findByHotelId(hotelId));
    }

    @PostMapping("/hotels/{hotelId}/room-types")
    public ResponseEntity<RoomType> createRoomType(@PathVariable Long hotelId, @RequestBody RoomType roomType) {
        User current = getCurrentUser();
        Hotel hotel = hotelRepository.findById(hotelId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy khách sạn."));
        if (!isAdmin(current) && (hotel.getOwner() == null || !hotel.getOwner().getId().equals(current.getId()))) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Không có quyền thêm loại phòng cho khách sạn này.");
        }
        roomType.setHotel(hotel);
        return ResponseEntity.ok(roomTypeRepository.save(roomType));
    }

    @PutMapping("/room-types/{id}")
    public ResponseEntity<RoomType> updateRoomType(@PathVariable Long id, @RequestBody RoomType request) {
        User current = getCurrentUser();
        RoomType roomType = roomTypeRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy loại phòng."));
        
        Hotel hotel = roomType.getHotel();
        if (hotel == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Loại phòng không liên kết với khách sạn nào.");
        }
        if (!isAdmin(current) && (hotel.getOwner() == null || !hotel.getOwner().getId().equals(current.getId()))) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Không có quyền cập nhật loại phòng của khách sạn này.");
        }
        
        if (request.getTypeName() != null) roomType.setTypeName(request.getTypeName());
        if (request.getDescription() != null) roomType.setDescription(request.getDescription());
        if (request.getBasePrice() != null) roomType.setBasePrice(request.getBasePrice());
        if (request.getMaxAdults() != null) roomType.setMaxAdults(request.getMaxAdults());
        if (request.getMaxChildren() != null) roomType.setMaxChildren(request.getMaxChildren());
        if (request.getTotalRooms() != null) roomType.setTotalRooms(request.getTotalRooms());
        if (request.getRoomSize() != null) roomType.setRoomSize(request.getRoomSize());

        return ResponseEntity.ok(roomTypeRepository.save(roomType));
    }

    @DeleteMapping("/room-types/{id}")
    public ResponseEntity<Void> deleteRoomType(@PathVariable Long id) {
        User current = getCurrentUser();
        RoomType roomType = roomTypeRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy loại phòng."));
        
        Hotel hotel = roomType.getHotel();
        if (hotel != null) {
            if (!isAdmin(current) && (hotel.getOwner() == null || !hotel.getOwner().getId().equals(current.getId()))) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Không có quyền xóa loại phòng của khách sạn này.");
            }
        }
        
        roomTypeRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
