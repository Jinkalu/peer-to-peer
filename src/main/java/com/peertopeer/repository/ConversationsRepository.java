package com.peertopeer.repository;

import com.peertopeer.entity.Conversations;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ConversationsRepository extends JpaRepository<Conversations, Long> {

/*    @Query("SELECT c FROM Conversations c JOIN UserConversation uc ON c.id = uc.conversationId WHERE uc.userId =:currentUserUUID AND status != 1 ORDER BY c.updatedAt DESC")
    Page<Conversations> findConversationsByUserUUID(String currentUserUUID, Pageable pageable);*/

/*   @Query("""
            SELECT c.id FROM Conversations c
                        JOIN Message m ON c.id = m.conversation.id 
                                    where 
                                                (m.receiverUUID=:receiverUUID AND m.senderUUID=:senderUUID)
                                                            OR 
                                                                        (m.receiverUUID=:senderUUID AND m.senderUUID=:receiverUUID)
            """)
    Long findConversationIdReceiverUUIDAndSenderUUID(String receiverUUID, String senderUUID);*/


    @Query("""
                SELECT c.id FROM Conversations c
                JOIN c.users u
                WHERE u.id IN (:sender, :receiver)
                GROUP BY c.id
                HAVING COUNT(DISTINCT u.id) = 2
            """)
    Optional<Long> findByUsers_IdAndUsers_Id(Long sender, Long receiver);
}
