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

@RestController
@RequestMapping("/api/admin/users")
@RequiredArgsConstructor
public class AdminUserController {

    private final AdminUserService adminUserService;
    private final UserRepository userRepository;

    private void checkAdmin() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getPrincipal() == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not authenticated");
        }
        String email = auth.getName();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found"));
        if (user.getRole() == null || !"ADMIN".equalsIgnoreCase(user.getRole().getRoleName())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Chỉ Quản trị viên hệ thống mới có quyền truy cập chức năng này.");
        }
    }

    @GetMapping
    public ResponseEntity<List<AdminUserResponse>> getAll(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String role) {
        checkAdmin();
        return ResponseEntity.ok(adminUserService.getAll(keyword, role));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<AdminUserResponse> toggleStatus(@PathVariable Long id) {
        checkAdmin();
        return ResponseEntity.ok(adminUserService.toggleStatus(id));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        checkAdmin();
        adminUserService.delete(id);
        return ResponseEntity.noContent().build();
    }

    // Thêm phương thức POST để xóa nếu DELETE bị chặn
    @PostMapping("/{id}/delete")
    public ResponseEntity<Void> deletePost(@PathVariable Long id) {
        checkAdmin();
        adminUserService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
