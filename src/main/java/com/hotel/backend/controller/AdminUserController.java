package com.hotel.backend.controller;

import com.hotel.backend.dto.AdminUserResponse;
import com.hotel.backend.service.AdminUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import com.hotel.backend.model.User;
import com.hotel.backend.repository.UserRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import com.hotel.backend.dto.OwnerDetailsResponse;

@RestController
@RequestMapping("/api/admin/users")
@RequiredArgsConstructor
public class AdminUserController {

    private final AdminUserService adminUserService;
    private final UserRepository userRepository;

    private User ensureAdminOrOwner() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getPrincipal() == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not authenticated");
        }
        String email = auth.getName();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found"));
        if (user.getRole() == null || (!"ADMIN".equalsIgnoreCase(user.getRole().getRoleName()) && !"OWNER".equalsIgnoreCase(user.getRole().getRoleName()))) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Bạn không có quyền truy cập chức năng này.");
        }
        return user;
    }

    private void ensureAdmin() {
        User user = ensureAdminOrOwner();
        if (!"ADMIN".equalsIgnoreCase(user.getRole().getRoleName())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Thao tác này chỉ dành cho Quản trị viên hệ thống.");
        }
    }

    @GetMapping("/{id}/details")
    public ResponseEntity<OwnerDetailsResponse> getDetails(@PathVariable Long id) {
        ensureAdmin();
        return ResponseEntity.ok(adminUserService.getOwnerDetails(id));
    }

    @GetMapping
    public ResponseEntity<List<AdminUserResponse>> getAll(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String role) {
        ensureAdmin();
        return ResponseEntity.ok(adminUserService.getAll(keyword, role));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<AdminUserResponse> toggleStatus(@PathVariable Long id) {
        ensureAdmin();
        return ResponseEntity.ok(adminUserService.toggleStatus(id));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        ensureAdmin();
        adminUserService.delete(id);
        return ResponseEntity.noContent().build();
    }

    // Thêm phương thức POST để xóa nếu DELETE bị chặn
    @PostMapping("/{id}/delete")
    public ResponseEntity<Void> deletePost(@PathVariable Long id) {
        ensureAdmin();
        adminUserService.delete(id);
        return ResponseEntity.noContent().build();
    }
    @GetMapping("/my-customers")
    public ResponseEntity<List<AdminUserResponse>> getMyCustomers() {
        User user = ensureAdminOrOwner();
        return ResponseEntity.ok(adminUserService.getUsersForOwner(user.getId()));
    }
}
