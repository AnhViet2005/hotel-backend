package com.hotel.backend.controller;

import com.hotel.backend.dto.ChatMessageDTO;
import com.hotel.backend.model.ChatMessage;
import com.hotel.backend.model.User;
import com.hotel.backend.repository.ChatMessageRepository;
import com.hotel.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ChatController {

    private final SimpMessagingTemplate messagingTemplate;
    private final ChatMessageRepository chatMessageRepository;
    private final UserRepository userRepository;
    private final com.hotel.backend.service.NotificationService notificationService;

    @MessageMapping("/chat.send")
    public void sendMessage(@Payload ChatMessageDTO chatMessageDTO) {
        User sender = userRepository.findById(chatMessageDTO.senderId()).orElse(null);
        User receiver = userRepository.findById(chatMessageDTO.receiverId()).orElse(null);

        if (sender != null && receiver != null) {
            ChatMessage chatMessage = ChatMessage.builder()
                    .sender(sender)
                    .receiver(receiver)
                    .content(chatMessageDTO.content())
                    .isRead(false)
                    .build();
            
            ChatMessage saved = chatMessageRepository.save(chatMessage);
            
            // Gửi thông báo Notification (Pull Request style)
            notificationService.notifyNewChatMessage(saved);

            ChatMessageDTO response = new ChatMessageDTO(
                    saved.getId(),
                    sender.getId(),
                    sender.getFullName(),
                    receiver.getId(),
                    chatMessageDTO.content(),
                    false,
                    saved.getCreatedAt()
            );

            // Gửi tin nhắn tới người nhận thông qua socket riêng của họ
            messagingTemplate.convertAndSendToUser(
                    receiver.getEmail(), "/queue/messages", response);
            
            // Gửi lại xác nhận cho người gửi
            messagingTemplate.convertAndSendToUser(
                    sender.getEmail(), "/queue/messages", response);
        }
    }

    @GetMapping("/history/{receiverId}")
    public ResponseEntity<List<ChatMessageDTO>> getChatHistory(@PathVariable Long receiverId) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String currentEmail = auth.getName();
        User currentUser = userRepository.findByEmail(currentEmail).orElse(null);
        User otherUser = userRepository.findById(receiverId).orElse(null);

        if (currentUser != null && otherUser != null) {
            List<ChatMessage> messages = chatMessageRepository.findConversation(currentUser, otherUser);
            List<ChatMessageDTO> dtos = messages.stream().map(m -> new ChatMessageDTO(
                    m.getId(),
                    m.getSender().getId(),
                    m.getSender().getFullName(),
                    m.getReceiver().getId(),
                    m.getContent(),
                    m.getIsRead(),
                    m.getCreatedAt()
            )).collect(Collectors.toList());
            return ResponseEntity.ok(dtos);
        }
        return ResponseEntity.badRequest().build();
    }

    @GetMapping("/active-contacts")
    public ResponseEntity<List<java.util.Map<String, Object>>> getActiveContacts() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String currentEmail = auth.getName();
        User currentUser = userRepository.findByEmail(currentEmail).orElse(null);

        if (currentUser != null) {
            List<User> contacts = chatMessageRepository.findActiveChats(currentUser);
            List<java.util.Map<String, Object>> result = contacts.stream()
                .filter(u -> !u.getId().equals(currentUser.getId()))
                .map(u -> {
                    java.util.Map<String, Object> map = new java.util.HashMap<>();
                    map.put("id", u.getId());
                    map.put("fullName", u.getFullName());
                    map.put("email", u.getEmail());
                    map.put("avatarUrl", u.getAvatarUrl());
                    return map;
                }).collect(Collectors.toList());
            return ResponseEntity.ok(result);
        }
        return ResponseEntity.badRequest().build();
    }
}
