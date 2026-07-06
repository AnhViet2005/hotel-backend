package com.hotel.backend.controller;

import com.hotel.backend.model.Post;
import com.hotel.backend.repository.PostRepository;
import com.hotel.backend.model.User;
import com.hotel.backend.repository.UserRepository;
import com.hotel.backend.model.Hotel;
import com.hotel.backend.repository.HotelRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

import java.util.List;

@RestController
@RequestMapping("/api/admin/posts")
@RequiredArgsConstructor
public class AdminPostController {

    private final PostRepository postRepository;
    private final UserRepository userRepository;
    private final HotelRepository hotelRepository;

    private void checkAdmin() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getPrincipal() == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not authenticated");
        }
        String email = auth.getName();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found"));
        if (user.getRole() == null || !"ADMIN".equalsIgnoreCase(user.getRole().getRoleName())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Chỉ Admin mới có quyền thực hiện chức năng này.");
        }
    }

    @GetMapping
    public ResponseEntity<List<Post>> getAll() {
        checkAdmin();
        return ResponseEntity.ok(postRepository.findAllByOrderByDisplayOrderAsc());
    }

    @PostMapping
    public ResponseEntity<Post> create(@RequestBody Post post) {
        checkAdmin();
        if (post.getHotels() != null && !post.getHotels().isEmpty()) {
            java.util.Set<Hotel> fullHotels = new java.util.HashSet<>();
            for (Hotel h : post.getHotels()) {
                if (h.getId() != null) {
                    hotelRepository.findById(h.getId()).ifPresent(fullHotels::add);
                }
            }
            post.setHotels(fullHotels);
        }
        return ResponseEntity.ok(postRepository.save(post));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Post> update(@PathVariable Long id, @RequestBody Post postDetails) {
        checkAdmin();
        Post post = postRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy bài viết"));
        
        post.setTitle(postDetails.getTitle());
        post.setSubtitle(postDetails.getSubtitle());
        post.setContent(postDetails.getContent());
        post.setImageUrl(postDetails.getImageUrl());
        post.setDisplayOrder(postDetails.getDisplayOrder());
        post.setIsActive(postDetails.getIsActive());
        
        if (postDetails.getHotels() != null) {
            java.util.Set<Hotel> fullHotels = new java.util.HashSet<>();
            for (Hotel h : postDetails.getHotels()) {
                if (h.getId() != null) {
                    hotelRepository.findById(h.getId()).ifPresent(fullHotels::add);
                }
            }
            post.setHotels(fullHotels);
        } else {
            post.getHotels().clear();
        }
        
        return ResponseEntity.ok(postRepository.save(post));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        checkAdmin();
        postRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
