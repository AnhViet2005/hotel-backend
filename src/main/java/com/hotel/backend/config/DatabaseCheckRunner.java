package com.hotel.backend.config;

import com.hotel.backend.model.User;
import com.hotel.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class DatabaseCheckRunner implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) throws Exception {
        System.out.println("[AUTH CHECK] Resetting all user passwords to '123456' for testing...");
        List<User> users = userRepository.findAll();
        String newPasswordHash = passwordEncoder.encode("123456");
        
        for (User user : users) {
            user.setPasswordHash(newPasswordHash);
        }
        
        userRepository.saveAll(users);
        System.out.println("[AUTH CHECK] Successfully updated " + users.size() + " users.");
    }
}
