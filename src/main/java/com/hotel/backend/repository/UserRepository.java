package com.hotel.backend.repository;

import com.hotel.backend.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);
    java.util.List<User> findByRoleRoleName(String roleName);
    java.util.List<User> findByRoleRoleNameIgnoreCase(String roleName);
    Optional<User> findByResetToken(String resetToken);
}
