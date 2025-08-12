package com.peertopeer.repository;

import com.peertopeer.entity.Conversations;
import com.peertopeer.entity.Message;
import com.peertopeer.enums.MessageStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
public interface MessageRepository extends JpaRepository<Message, Long> {

    List<Message> findByConversation_Id(Long id);

    @Transactional
    @Modifying
    @Query(value = """
            UPDATE messages m
            SET status = :status
            FROM conversation_user cu
            WHERE m.conversation_id = cu.conversation_id
              AND cu.user_id = :userId
              AND m.senderuuid != CAST(:userId AS TEXT)
              AND m.status = :status1
            """, nativeQuery = true)
    void updateStatusBySenderUUIDAndStatus(String status, Long userId, String status1);

    @Transactional
    @Modifying
    @Query("update Message m set m.status = ?1 where m.conversation = ?2 and m.senderUUID <> ?3 and m.status = ?4")
    void updateStatusByConversationAndSenderUUIDNotAndStatus(MessageStatus status, Conversations conversation, String senderUUID, MessageStatus status1);

    @Transactional
    @Modifying
    @Query("update Message m set m.status = ?1 where m.conversation.id = ?2 and m.senderUUID <> ?3 and m.status = ?4")
    void updateStatusByConversation_IdAndSenderUUIDNotAndStatus(MessageStatus messageStatus, long convoId, String user, MessageStatus messageStatus1);


    @Query(value = """
            SELECT COUNT(*)
            FROM messages m
            JOIN conversation_user cu ON m.conversation_id = cu.conversation_id
            WHERE cu.user_id = :receiver
              AND m.senderuuid = :sender
              AND m.status = 'DELIVERED'
            """, nativeQuery = true)
    Long countUnreadMessages(String sender, Long receiver);

    @Transactional
    @Modifying
    @Query("update Message m set m.reaction = ?1 where m.id = ?2")
    void updateReactionById(String reaction, Long id);


    @Modifying
    @Transactional
    @Query(value = """
            UPDATE messages SET
                status = CASE
                    WHEN id = :messageId THEN 'DELETED'
                    ELSE status
                END,
                replay_to_id = CASE
                    WHEN replay_to_id = :messageId THEN NULL
                    ELSE replay_to_id
                END
            WHERE id = :messageId OR replay_to_id = :messageId
            """, nativeQuery = true)
    void deleteMessageAndClearReplies(@Param("messageId") Long messageId);
}
