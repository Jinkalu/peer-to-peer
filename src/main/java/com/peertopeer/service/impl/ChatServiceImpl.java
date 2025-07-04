package com.peertopeer.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.peertopeer.config.handlers.ChatWebSocketHandler;
import com.peertopeer.entity.Message;
import com.peertopeer.repository.ConversationsRepository;
import com.peertopeer.repository.MessageRepository;
import com.peertopeer.repository.UserConversationRepository;
import com.peertopeer.service.ChatService;
import com.peertopeer.service.PresenceService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ChatServiceImpl implements ChatService {

    private final MessageRepository messageRepository;

    private final UserConversationRepository userConversationRepository;
    private final ConversationsRepository conversationsRepository;
    private final PresenceService presenceService;

    @Override
    public List<Message> getChatHistory(String sender, String receiver) {
        return messageRepository.findChatBetween(receiver, sender,
                PageRequest.of(0, 10)).getContent();
    }

    @Override
    @Transactional
    public void markMessagesAsSeen(String sender, String receiver) {
        messageRepository.markAsSeen(sender, receiver);

        // ✅ Notify sender over WebSocket
        WebSocketSession senderSession = new ChatWebSocketHandler(conversationsRepository,
                messageRepository, userConversationRepository, presenceService).getUserSession(sender);
        if (senderSession != null && senderSession.isOpen()) {
            Map<String, Object> seenStatus = Map.of(
                    "type", "status",
                    "status", "SEEN",
                    "from", receiver // who read it
            );
            try {
                senderSession.sendMessage(new TextMessage(new ObjectMapper().writeValueAsString(seenStatus)));
            } catch (IOException e) {
                System.out.println(e.getMessage());
            }
        }
    }
}
