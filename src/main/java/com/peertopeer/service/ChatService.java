package com.peertopeer.service;

import com.peertopeer.entity.Conversations;
import com.peertopeer.entity.Message;
import com.peertopeer.enums.MessageReaction;
import com.peertopeer.enums.MessageStatus;
import com.peertopeer.vo.GroupCreationVO;
import com.peertopeer.vo.MessageVO;

import java.util.List;


public interface ChatService {

    List<MessageVO> getChatHistory(String conversationId);

//    void markMessagesAsSeen(String sender, String receiver);

    Long create(String user, String target);

    Long saveMessage(String conversationId, String fromUser, String msg, MessageStatus status);

    void updateMessageStatus(String userId);


    void updateMessageChatStatus(long value, String user);

    Long unreadCount(String target, String connectedUser);

    void messageReaction(Long messageId, String reaction);

    void deleteMessage(Long messageId);
}
