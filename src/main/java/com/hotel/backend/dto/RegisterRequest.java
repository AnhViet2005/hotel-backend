package com.hotel.backend.dto;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RegisterRequest {
    private String fullName;
    private String email;
    private String password;
    private String phone;
    private String hotelName; // For partner registration
}
