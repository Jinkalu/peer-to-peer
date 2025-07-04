package com.peertopeer.repository;

import com.peertopeer.entity.Message;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MessageRepository extends JpaRepository<Message, Long> {


//    @Query("SELECT m FROM Message m WHERE (m.receiverUUID = :user1 AND m.senderUUID = :user2) OR (m.receiverUUID = :user2 AND m.senderUUID = :user1) ORDER BY id DESC")
    @Query("""
            SELECT m FROM Message m WHERE
            (m.receiverUUID = :user1 AND m.senderUUID = :user2)
            OR (m.receiverUUID = :user2 AND m.senderUUID = :user1) ORDER BY id ASC
            """)
    Page<Message> findChatBetween(@Param("user1") String user1, @Param("user2") String user2, Pageable pageable);


    @Modifying
    @Query("UPDATE Message m SET m.status = 'SEEN' WHERE m.senderUUID = :sender AND m.receiverUUID = :receiver AND m.status = 'SEND' ")
    void markAsSeen(@Param("sender") String sender, @Param("receiver") String receiver);

    @Query("""
            SELECT m FROM Message m WHERE
            (m.receiverUUID = :user1 AND m.senderUUID = :user2)
            OR (m.receiverUUID = :user2 AND m.senderUUID = :user1) ORDER BY id ASC
            """)
    List<Message> findBySenderUUIDAndReceiverUUID(String senderUUID, String receiverUUID);
}
