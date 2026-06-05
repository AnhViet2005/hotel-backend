package com.hotel.backend.repository;

import com.hotel.backend.model.Favorite;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface FavoriteRepository extends JpaRepository<Favorite, Favorite.FavoriteId> {
    List<Favorite> findByUserId(Long userId);
}
