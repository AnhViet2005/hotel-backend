package com.hotel.backend.controller;

import com.hotel.backend.dto.PublicPostResponse;
import com.hotel.backend.service.PostService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

import java.util.List;

@RestController
@RequestMapping("/api/public/posts")
@RequiredArgsConstructor
public class PublicPostController {

    private final PostService postService;

    @GetMapping
    public ResponseEntity<List<PublicPostResponse>> getActivePosts() {
        return ResponseEntity.ok(postService.getAllActivePosts());
    }

    @GetMapping("/{id}")
    public ResponseEntity<PublicPostResponse> getPostById(@PathVariable Long id) {
        PublicPostResponse post = postService.getPostById(id);
        if (post == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy bài viết");
        }
        return ResponseEntity.ok(post);
    }
}
