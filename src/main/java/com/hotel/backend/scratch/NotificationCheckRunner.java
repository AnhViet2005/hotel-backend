package com.hotel.backend.scratch;

import com.hotel.backend.repository.NotificationRepository;
import com.hotel.backend.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class NotificationCheckRunner implements CommandLineRunner {
    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;

    public NotificationCheckRunner(NotificationRepository notificationRepository, UserRepository userRepository) {
        this.notificationRepository = notificationRepository;
        this.userRepository = userRepository;
    }

    @Override
    public void run(String... args) throws Exception {
        System.out.println("======= NOTIFICATION CHECK =======");
        System.out.println("Total notifications: " + notificationRepository.count());
        System.out.println("======= USERS CHECK =======");
        userRepository.findAll().forEach(u -> {
            System.out.println("User: " + u.getEmail() + " | Role: " + (u.getRole() != null ? u.getRole().getRoleName() : "NULL"));
        });
        System.out.println("==================================");
    }
}
