package com.peertopeer.service;

import com.peertopeer.enums.MessageStatus;
import com.peertopeer.vo.MessageVO;

import java.util.List;


public interface ChatService {

    List<MessageVO> getChatHistory(String conversationId);

//    void markMessagesAsSeen(String sender, String receiver);

    Long create(String user, String target);

    void saveMessage(String conversationId, String messageId, String fromUser,
                       String msg, String replayTo, MessageStatus status);

    void updateMessageStatus(String userId);


    void updateMessageChatStatus(long value, String user);

    Long unreadCount(String target, String connectedUser);

    void messageReaction(String messageId, String reaction, String type);

    void deleteMessage(String messageId);
}
