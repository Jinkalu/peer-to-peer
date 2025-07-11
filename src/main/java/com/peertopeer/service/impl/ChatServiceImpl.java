package com.peertopeer.service.impl;

import com.peertopeer.config.handlers.ChatWebSocketHandler;
import com.peertopeer.entity.Conversations;
import com.peertopeer.entity.Message;
import com.peertopeer.enums.ConversationType;
import com.peertopeer.enums.MessageStatus;
import com.peertopeer.repository.ConversationsRepository;
import com.peertopeer.repository.MessageRepository;
import com.peertopeer.repository.UserRepository;
import com.peertopeer.service.ChatService;
import com.peertopeer.service.PresenceService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
@RequiredArgsConstructor
public  class ChatServiceImpl implements ChatService {

    private final MessageRepository messageRepository;
    private final ConversationsRepository conversationsRepository;
    private final UserRepository userRepository;


    @Override
    public List<Message> getChatHistory(String conversationId) {
        return messageRepository.findByConversation_Id(Long.valueOf(conversationId));

    }


    @Override
    public Long create(String user, String target) {
        Long sender = Long.valueOf(user);
        Long receiver = Long.valueOf(target);
        Optional<Long> byUsersIdAndUsersId = conversationsRepository.findByUsers_IdAndUsers_Id(sender, receiver);
        return byUsersIdAndUsersId.orElseGet(() -> conversationsRepository.saveAndFlush(Conversations.builder()
                .type(ConversationType.PRIVATE)
                .users(Set.of(userRepository.findById(sender).get(),
                        userRepository.findById(receiver).get()))
                .build()).getId());

    }

    @Override
    @Transactional(rollbackOn = Exception.class)
    public Long saveMessage(String conversationId, String fromUser, String msg, MessageStatus status) {
        return messageRepository.saveAndFlush(Message.builder()
                .message(msg)
                .conversation(conversationsRepository.findById(Long.valueOf(conversationId)).get())
                .senderUUID(fromUser)
                .status(status)
                .build()).getId();
    }

    @Override
    public void updateMessageStatus(String userId) {
        messageRepository.updateStatusBySenderUUIDAndStatus(MessageStatus.DELIVERED.name(), Long.valueOf(userId), MessageStatus.SEND.name());
    }

    @Override
    public void updateMessageChatStatus(long convoId, String user) {
        messageRepository.updateStatusByConversation_IdAndSenderUUIDNotAndStatus(MessageStatus.SEEN,convoId,user,MessageStatus.DELIVERED);
    }

    @Override
    public List<Message> createGroup(String userId) {
        conversationsRepository.saveAndFlush(Conversations.builder()
                        .users(Set.of(userRepository.findById(Long.valueOf(userId)).get()))
                        .conversationName("")
                .build());
        return List.of();
    }


}
