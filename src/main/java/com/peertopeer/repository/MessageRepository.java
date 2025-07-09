package com.peertopeer.repository;

import com.peertopeer.entity.Conversations;
import com.peertopeer.entity.Message;
import com.peertopeer.enums.MessageStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
public interface MessageRepository extends JpaRepository<Message, Long> {

/*    @Query("""
            SELECT m FROM Message m WHERE
            (m.receiverUUID = :user1 AND m.senderUUID = :user2)
            OR (m.receiverUUID = :user2 AND m.senderUUID = :user1) ORDER BY id ASC
            """)
    Page<Message> findChatBetween(@Param("user1") String user1, @Param("user2") String user2, Pageable pageable);*/


/*    @Modifying
    @Query("UPDATE Message m SET m.status = 'SEEN' WHERE m.senderUUID = :sender AND m.receiverUUID = :receiver AND m.status = 'SEND' ")
    void markAsSeen(@Param("sender") String sender, @Param("receiver") String receiver);*/

/*    @Query("""
            SELECT m FROM Message m WHERE
            (m.receiverUUID = :user1 AND m.senderUUID = :user2)
            OR (m.receiverUUID = :user2 AND m.senderUUID = :user1) ORDER BY id ASC
            """)
    List<Message> findBySenderUUIDAndReceiverUUID(String senderUUID, String receiverUUID);*/

/*    @Transactional
    @Modifying
    @Query("update Message m set m.status = 'SEEN' where m.receiverUUID = ?1 and m.status = ?2 and m.senderUUID = ?3")
    void updateStatusByReceiverUUIDAndStatusAndSenderUUID(String receiverUUID, MessageStatus status, String senderUUID);
*/

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
            """,nativeQuery = true)
    void updateStatusBySenderUUIDAndStatus(String status, Long userId, String status1);

    @Transactional
    @Modifying
    @Query("update Message m set m.status = ?1 where m.conversation = ?2 and m.senderUUID <> ?3 and m.status = ?4")
    void updateStatusByConversationAndSenderUUIDNotAndStatus(MessageStatus status, Conversations conversation, String senderUUID, MessageStatus status1);

    @Transactional
    @Modifying
    @Query("update Message m set m.status = ?1 where m.conversation.id = ?2 and m.senderUUID <> ?3 and m.status = ?4")
    void updateStatusByConversation_IdAndSenderUUIDNotAndStatus(MessageStatus messageStatus, long convoId, String user, MessageStatus messageStatus1);
}
