package com.hotel.backend.service;

import com.hotel.backend.config.JwtService;
import com.hotel.backend.dto.AuthResponse;
import com.hotel.backend.dto.LoginRequest;
import com.hotel.backend.dto.RegisterRequest;
import com.hotel.backend.model.Role;
import com.hotel.backend.model.User;
import com.hotel.backend.repository.RoleRepository;
import com.hotel.backend.repository.UserRepository;
import com.hotel.backend.repository.HotelRepository;
import com.hotel.backend.model.Hotel;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor

public class AuthService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final HotelRepository hotelRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final UserDetailsService userDetailsService;
    private final EmailService emailService;

    public AuthResponse registerAdmin(RegisterRequest request) {
        Role adminRole = roleRepository.findByRoleName("ADMIN")
                .orElseGet(() -> roleRepository.save(Role.builder().roleName("ADMIN").build()));

        var user = User.builder()
                .fullName(request.getFullName())
                .email(request.getEmail())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .phone(request.getPhone())
                .role(adminRole)
                .isActive(true)
                .build();

        userRepository.save(user);
        userRepository.flush();        
        UserDetails userDetails = userDetailsService.loadUserByUsername(user.getEmail());
        var jwtToken = jwtService.generateToken(userDetails);
        
        return AuthResponse.builder()
                .token(jwtToken)
                .email(user.getEmail())
                .fullName(user.getFullName())
                .role("ADMIN")
                .phone(user.getPhone())
                .id(user.getId())
                .build();
    }

    public AuthResponse registerUser(RegisterRequest request) {
        Role userRole = roleRepository.findByRoleName("USER")
                .orElseGet(() -> roleRepository.save(Role.builder().roleName("USER").build()));

        var user = User.builder()
                .fullName(request.getFullName())
                .email(request.getEmail())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .phone(request.getPhone())
                .role(userRole)
                .isActive(true)
                .build();

        userRepository.save(user);
        userRepository.flush();
        UserDetails userDetails = userDetailsService.loadUserByUsername(user.getEmail());
        var jwtToken = jwtService.generateToken(userDetails);

        return AuthResponse.builder()
                .token(jwtToken)
                .email(user.getEmail())
                .fullName(user.getFullName())
                .role("USER")
                .phone(user.getPhone())
                .id(user.getId())
                .build();
    }

    public AuthResponse registerOwner(RegisterRequest request) {
        Role ownerRole = roleRepository.findByRoleName("OWNER")
                .orElseGet(() -> roleRepository.save(Role.builder().roleName("OWNER").build()));

        var user = User.builder()
                .fullName(request.getFullName())
                .email(request.getEmail())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .phone(request.getPhone())
                .role(ownerRole)
                .isActive(true)
                .build();

        userRepository.save(user);

        // Tạo khách sạn mặc định từ tên khách sạn được cung cấp
        if (request.getHotelName() != null && !request.getHotelName().isEmpty()) {
            Hotel hotel = Hotel.builder()
                    .hotelName(request.getHotelName())
                    .owner(user)
                    .isActive(true) // Có thể để false để chờ duyệt, nhưng demo để true
                    .email(request.getEmail())
                    .phone(request.getPhone())
                    .build();
            hotelRepository.save(hotel);
        }

        userRepository.flush();
        UserDetails userDetails = userDetailsService.loadUserByUsername(user.getEmail());
        var jwtToken = jwtService.generateToken(userDetails);

        return AuthResponse.builder()
                .token(jwtToken)
                .email(user.getEmail())
                .fullName(user.getFullName())
                .role("OWNER")
                .phone(user.getPhone())
                .id(user.getId())
                .build();
    }

    public AuthResponse login(LoginRequest request) {
        // Check if email exists
        userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED, "Email không tồn tại trong hệ thống."));

        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            request.getEmail(),
                            request.getPassword()
                    )
            );
        } catch (BadCredentialsException e) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Mật khẩu không đúng. Vui lòng thử lại.");
        }

        var user = userRepository.findByEmail(request.getEmail()).orElseThrow();

        UserDetails userDetails = userDetailsService.loadUserByUsername(user.getEmail());
        var jwtToken = jwtService.generateToken(userDetails);

        return AuthResponse.builder()
                .token(jwtToken)
                .email(user.getEmail())
                .fullName(user.getFullName())
                .role(user.getRole().getRoleName())
                .phone(user.getPhone())
                .id(user.getId())
                .build();
    }

    public String forgotPassword(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Email không tồn tại trong hệ thống."));

        String token = java.util.UUID.randomUUID().toString();
        user.setResetToken(token);
        user.setResetTokenExpiry(java.time.LocalDateTime.now().plusMinutes(15));
        userRepository.save(user);

        String resetLink = "https://hotel-booking-inky-eta.vercel.app/reset-password?token=" + token;
        emailService.sendPasswordResetEmail(user.getEmail(), resetLink);
        
        return "Yêu cầu khôi phục mật khẩu đã được xử lý. Do môi trường Cloud chặn cổng gửi mail SMTP, bạn có thể bấm trực tiếp vào đường dẫn thử nghiệm dưới đây để đặt lại mật khẩu:\n\n" + resetLink;
    }

    public void resetPassword(String token, String newPassword) {
        User user = userRepository.findByResetToken(token)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Token không hợp lệ hoặc đã hết hạn."));

        if (user.getResetTokenExpiry().isBefore(java.time.LocalDateTime.now())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Token đã hết hạn.");
        }

        user.setPasswordHash(passwordEncoder.encode(newPassword));
        user.setResetToken(null);
        user.setResetTokenExpiry(null);
        userRepository.save(user);
    }
}
