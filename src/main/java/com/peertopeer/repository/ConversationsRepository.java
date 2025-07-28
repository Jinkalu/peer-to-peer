package com.peertopeer.repository;

import com.peertopeer.entity.Conversations;
import com.peertopeer.entity.Users;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Repository
public interface ConversationsRepository extends JpaRepository<Conversations, Long> {

    @Query("""
                SELECT c.id FROM Conversations c
                JOIN c.users u
                WHERE u.id IN (:sender, :receiver)
                AND c.type = 'PRIVATE_CHAT'
                GROUP BY c.id
                HAVING COUNT(DISTINCT u.id) = 2
            """)
    Optional<Long> findByUsers_IdAndUsers_Id(Long sender, Long receiver);

    @Transactional
    @Modifying
    @Query("update Conversations c set c.updatedAt = ?1 where c.id = ?2")
    void updateUpdatedAtById(Long updatedAt, Long id);

    @Query("""
            SELECT c.users FROM Conversations c
            WHERE c.id = :conversationId
            """)
    Page<Users> groupMembers(Long conversationId, Pageable pageable);


    @Query("""
                SELECT c FROM Conversations c
                JOIN c.users u
                WHERE u.id = :userId
            """)
    List<Conversations> findByUsers_Id(Long userId);

    @Query("""
            SELECT u FROM Conversations c
            JOIN c.users u
            WHERE u.id != :userId
            AND
            c.id = :conversationId
            """)
    Users getPeerUser(Long userId, Long conversationId);

    @Query(value = """
            SELECT count(m.*) FROM conversations c
            JOIN messages m ON c.id = m.conversation_id
            WHERE
            c.id = :conversationId AND
            m.senderUUID != :currentUserUid AND
            m.status = 'DELIVERED'
            """, nativeQuery = true)
    Long unreadCount(String currentUserUid, Long conversationId);
}
