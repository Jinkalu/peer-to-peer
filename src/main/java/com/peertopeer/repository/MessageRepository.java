package com.peertopeer.repository;

import com.peertopeer.entity.Message;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

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

}
