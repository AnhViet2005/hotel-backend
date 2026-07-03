package com.hotel.backend.service;

import com.hotel.backend.dto.AdminUserResponse;
import com.hotel.backend.model.User;
import com.hotel.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@SuppressWarnings("null")
public class AdminUserService {

    private final UserRepository userRepository;
    private final jakarta.persistence.EntityManager entityManager;

    public List<AdminUserResponse> getAll(String keyword, String role) {
        List<User> users = userRepository.findAll();

        if (keyword != null && !keyword.isBlank()) {
            String kw = keyword.toLowerCase();
            users = users.stream()
                    .filter(u -> (u.getFullName() != null && u.getFullName().toLowerCase().contains(kw))
                            || (u.getEmail() != null && u.getEmail().toLowerCase().contains(kw)))
                    .collect(Collectors.toList());
        }

        if (role != null && !role.isBlank() && !role.equals("ALL")) {
            users = users.stream()
                    .filter(u -> u.getRole() != null && u.getRole().getRoleName().equalsIgnoreCase(role))
                    .collect(Collectors.toList());
        }

        return users.stream().map(this::toResponse).collect(Collectors.toList());
    }

    /** Lấy danh sách khách hàng đã từng đặt phòng tại các khách sạn của chủ sở hữu này */
    public List<AdminUserResponse> getUsersForOwner(Long ownerId) {
        // 1. Lấy những người dùng có tài khoản User chính thức
        String jpqlUsers = "SELECT DISTINCT b.user FROM Booking b WHERE b.hotel.owner.id = :ownerId AND b.user IS NOT NULL";
        List<User> registeredUsers = entityManager.createQuery(jpqlUsers, User.class)
                .setParameter("ownerId", ownerId)
                .getResultList();
        
        List<AdminUserResponse> result = registeredUsers.stream()
                .map(this::toResponse)
                .collect(Collectors.toList());

        // 2. Lấy những khách hàng đặt phòng (có thể là khách vãng lai không có user_id)
        String jpqlGuests = "SELECT DISTINCT b.guestName, b.guestEmail, b.guestPhone FROM Booking b WHERE b.hotel.owner.id = :ownerId AND b.user IS NULL";
        List<Object[]> guests = entityManager.createQuery(jpqlGuests, Object[].class)
                .setParameter("ownerId", ownerId)
                .getResultList();

        for (Object[] guest : guests) {
            result.add(AdminUserResponse.builder()
                    .fullName((String) guest[0])
                    .email((String) guest[1])
                    .phone((String) guest[2])
                    .role("GUEST")
                    .isActive(true)
                    .build());
        }
        
        return result;
    }

    @Transactional
    public AdminUserResponse toggleStatus(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy người dùng."));
        user.setIsActive(!Boolean.TRUE.equals(user.getIsActive()));
        return toResponse(userRepository.save(user));
    }

    /**
     * Xóa vĩnh viễn người dùng và tất cả dữ liệu liên quan.
     * Sử dụng Native SQL để đảm bảo xóa sạch sành sanh các ràng buộc khóa ngoại.
     */
    @Transactional
    public void delete(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy người dùng."));
        
        // Không cho phép xóa Admin hệ thống để bảo vệ dữ liệu
        if (user.getRole() != null && "ADMIN".equalsIgnoreCase(user.getRole().getRoleName())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Không thể xóa tài khoản Quản trị viên.");
        }

        try {
            // Tắt kiểm tra khóa ngoại (MySQL syntax)
            entityManager.createNativeQuery("SET FOREIGN_KEY_CHECKS = 0").executeUpdate();
            
            // XÓA DỮ LIỆU LIÊN QUAN ĐẾN KHÁCH SẠN (Nếu user là chủ khách sạn)
            String hotelIdsQuery = "SELECT id FROM hotels WHERE owner_id = :id";
            
            // Xóa ảnh, tiện nghi, chính sách, thống kê, khuyến mãi của khách sạn
            entityManager.createNativeQuery("DELETE FROM hotel_images WHERE hotel_id IN (" + hotelIdsQuery + ")").setParameter("id", id).executeUpdate();
            entityManager.createNativeQuery("DELETE FROM hotel_amenities WHERE hotel_id IN (" + hotelIdsQuery + ")").setParameter("id", id).executeUpdate();
            entityManager.createNativeQuery("DELETE FROM hotel_policies WHERE hotel_id IN (" + hotelIdsQuery + ")").setParameter("id", id).executeUpdate();
            entityManager.createNativeQuery("DELETE FROM hotel_statistics WHERE hotel_id IN (" + hotelIdsQuery + ")").setParameter("id", id).executeUpdate();
            entityManager.createNativeQuery("DELETE FROM promotions WHERE hotel_id IN (" + hotelIdsQuery + ")").setParameter("id", id).executeUpdate();
            
            // Xóa các loại phòng và dữ liệu liên quan (ảnh phòng, lịch phòng)
            String roomTypeIdsQuery = "SELECT id FROM room_types WHERE hotel_id IN (" + hotelIdsQuery + ")";
            entityManager.createNativeQuery("DELETE FROM room_images WHERE room_type_id IN (" + roomTypeIdsQuery + ")").setParameter("id", id).executeUpdate();
            entityManager.createNativeQuery("DELETE FROM room_calendar WHERE room_type_id IN (" + roomTypeIdsQuery + ")").setParameter("id", id).executeUpdate();
            entityManager.createNativeQuery("DELETE FROM booking_rooms WHERE room_type_id IN (" + roomTypeIdsQuery + ")").setParameter("id", id).executeUpdate();
            entityManager.createNativeQuery("DELETE FROM room_types WHERE hotel_id IN (" + hotelIdsQuery + ")").setParameter("id", id).executeUpdate();
            
            // XÓA DỮ LIỆU ĐẶT PHÒNG (Bookings)
            String bookingIdsQuery = "SELECT id FROM bookings WHERE user_id = :id";
            entityManager.createNativeQuery("DELETE FROM payments WHERE booking_id IN (" + bookingIdsQuery + ")").setParameter("id", id).executeUpdate();
            entityManager.createNativeQuery("DELETE FROM booking_rooms WHERE booking_id IN (" + bookingIdsQuery + ")").setParameter("id", id).executeUpdate();
            entityManager.createNativeQuery("DELETE FROM reviews WHERE booking_id IN (" + bookingIdsQuery + ")").setParameter("id", id).executeUpdate();
            entityManager.createNativeQuery("DELETE FROM bookings WHERE user_id = :id").setParameter("id", id).executeUpdate();
            
            // XÓA DỮ LIỆU CÁ NHÂN (Favorites, Reviews trực tiếp)
            entityManager.createNativeQuery("DELETE FROM favorites WHERE user_id = :id").setParameter("id", id).executeUpdate();
            entityManager.createNativeQuery("DELETE FROM reviews WHERE user_id = :id").setParameter("id", id).executeUpdate();
            
            // XÓA BẢN GHI CHÍNH
            entityManager.createNativeQuery("DELETE FROM hotels WHERE owner_id = :id").setParameter("id", id).executeUpdate();
            entityManager.createNativeQuery("DELETE FROM users WHERE id = :id").setParameter("id", id).executeUpdate();
            
            // Bật lại kiểm tra khóa ngoại
            entityManager.createNativeQuery("SET FOREIGN_KEY_CHECKS = 1").executeUpdate();
            
        } catch (Exception e) {
            entityManager.createNativeQuery("SET FOREIGN_KEY_CHECKS = 1").executeUpdate();
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Lỗi khi xóa dữ liệu: " + e.getMessage());
        }
    }

    private AdminUserResponse toResponse(User user) {
        return AdminUserResponse.builder()
                .id(user.getId())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .phone(user.getPhone())
                .role(user.getRole() != null ? user.getRole().getRoleName() : "UNKNOWN")
                .isActive(user.getIsActive())
                .createdAt(user.getCreatedAt())
                .avatarUrl(user.getAvatarUrl() != null ? user.getAvatarUrl() : "")
                .build();
    }
}
