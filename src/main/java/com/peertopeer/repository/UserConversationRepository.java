package com.peertopeer.repository;

import com.peertopeer.entity.UserConversation;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserConversationRepository extends JpaRepository<UserConversation, Long> {
}
