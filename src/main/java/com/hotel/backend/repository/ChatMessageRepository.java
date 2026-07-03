package com.hotel.backend.repository;

import com.hotel.backend.model.ChatMessage;
import com.hotel.backend.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {
    
    @Query("SELECT m FROM ChatMessage m WHERE " +
           "(m.sender = :user1 AND m.receiver = :user2) OR " +
           "(m.sender = :user2 AND m.receiver = :user1) " +
           "ORDER BY m.createdAt ASC")
    List<ChatMessage> findConversation(@Param("user1") User user1, @Param("user2") User user2);

    @Query("SELECT DISTINCT m.sender FROM ChatMessage m WHERE m.receiver = :user UNION " +
           "SELECT DISTINCT m.receiver FROM ChatMessage m WHERE m.sender = :user")
    List<User> findActiveChats(@Param("user") User user);
    
    long countByReceiverAndIsReadFalse(User receiver);
}
