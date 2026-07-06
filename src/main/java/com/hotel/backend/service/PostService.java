package com.hotel.backend.service;

import com.hotel.backend.dto.PublicHotelResponse;
import com.hotel.backend.dto.PublicPostResponse;
import com.hotel.backend.model.Post;
import com.hotel.backend.repository.PostRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PostService {

    private final PostRepository postRepository;
    private final PublicHotelService publicHotelService;

    public List<PublicPostResponse> getAllActivePosts() {
        return postRepository.findByIsActiveOrderByDisplayOrderAsc(true)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public PublicPostResponse getPostById(Long id) {
        Post post = postRepository.findById(id).orElse(null);
        if (post == null) return null;
        return mapToResponse(post);
    }

    public PublicPostResponse mapToResponse(Post post) {
        java.util.List<PublicHotelResponse> hotelResponses = new java.util.ArrayList<>();
        if (post.getHotels() != null) {
            for (com.hotel.backend.model.Hotel h : post.getHotels()) {
                hotelResponses.add(publicHotelService.getHotelByIdSimple(h.getId()));
            }
        }

        return PublicPostResponse.builder()
                .id(post.getId())
                .title(post.getTitle())
                .subtitle(post.getSubtitle())
                .content(post.getContent())
                .imageUrl(post.getImageUrl())
                .displayOrder(post.getDisplayOrder())
                .createdAt(post.getCreatedAt())
                .hotels(hotelResponses)
                .build();
    }
}
