package com.hotel.backend.repository;

import com.hotel.backend.model.ContactInfo;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface ContactInfoRepository extends JpaRepository<ContactInfo, Long> {
    Optional<ContactInfo> findFirstByOrderByIdDesc();
}
