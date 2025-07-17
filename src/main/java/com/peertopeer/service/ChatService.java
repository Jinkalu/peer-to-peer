package com.peertopeer.service;

import com.peertopeer.entity.Conversations;
import com.peertopeer.entity.Message;
import com.peertopeer.enums.MessageStatus;
import com.peertopeer.vo.GroupCreationVO;

import java.util.List;


public interface ChatService {

    List<Message> getChatHistory(String conversationId);

//    void markMessagesAsSeen(String sender, String receiver);

    Long create(String user, String target);

    Long saveMessage(String conversationId, String fromUser, String msg, MessageStatus status);

    void updateMessageStatus(String userId);


    void updateMessageChatStatus(long value, String user);

    Conversations createGroup(GroupCreationVO conversationId);

    Long unreadCount(String target, String connectedUser);
}
