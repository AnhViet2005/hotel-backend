package com.hotel.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminUserResponse {
    private Long id;
    private String fullName;
    private String email;
    private String phone;
    private String role;
    private Boolean isActive;
    private LocalDateTime createdAt;
    private String avatarUrl;
}
