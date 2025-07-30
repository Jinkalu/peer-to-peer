package com.peertopeer.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.peertopeer.enums.MessageStatus;
import com.peertopeer.service.PresenceService;
import com.peertopeer.service.StatusService;
import com.peertopeer.vo.MessageResponseVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

import static com.peertopeer.socket.handlers.GroupChatWebSocketHandler.roomSessions;
import static com.peertopeer.socket.handlers.PrivateChatWebSocketHandler.getUserSession;
import static com.peertopeer.utils.PeerUtils.getParam;
import static com.peertopeer.utils.PeerUtils.getPrivateChatId;

@Slf4j
@Service
@RequiredArgsConstructor
public class StatusServiceImpl implements StatusService {

    private final PresenceService presenceService;

    @Override
    public void sendStatus(MessageStatus status, String sender,
                           String messageId, String conversationId) throws IOException {
        MessageResponseVO deliveredStatus = MessageResponseVO.builder()
                .conversationId(conversationId)
                .statusReceiver(sender)
                .msgId(messageId)
                .type("status")
                .status(status)
                .build();
        WebSocketSession senderSession = getUserSession(sender);
        if (senderSession != null && senderSession.isOpen()) {
            senderSession.sendMessage(new TextMessage(new ObjectMapper().writeValueAsString(deliveredStatus)));
        }
    }

    @Override
    public void handleTypingStatus(WebSocketSession session, Map<String,
            String> payload) throws IOException {
        String fromUser = (String) session.getAttributes().get("userId");
        String toUser = payload.get("receiver");


        String chatId = getPrivateChatId(fromUser, toUser);

        if (Objects.equals(payload.get("isTyping"), "true")) {
            presenceService.setTyping(chatId, fromUser);
        } else {
            presenceService.clearTyping(chatId, fromUser);
        }

        WebSocketSession peerSession = getUserSession(toUser);
        if (peerSession != null && peerSession.isOpen()) {
            MessageResponseVO typingMessage = MessageResponseVO.builder()
                    .type("typing")
                    .from(fromUser)
                    .isTyping(payload.get("isTyping"))
                    .build();
            String json = new ObjectMapper().writeValueAsString(typingMessage);
            synchronized (peerSession) {
                peerSession.sendMessage(new TextMessage(json));
            }
        }
    }

    @Override
    public void handleGroupTypingStatus(WebSocketSession session, Map<String, String> payload) {
        String conversationId = getParam(session, "conversationId");
        String sender = getParam(session, "sender");
        roomSessions.getOrDefault(conversationId, Collections.emptySet()).stream()
                .filter(peerSession -> peerSession.isOpen()
                        && !peerSession.getAttributes().get("user").equals(sender))
                .forEach(peerSession -> CompletableFuture.runAsync(() -> {
                    try {
                        MessageResponseVO message = MessageResponseVO.builder()
                                .type("typing")
                                .from(sender)
                                .isTyping(payload.get("isTyping"))
                                .build();
                        String json = new ObjectMapper().writeValueAsString(message);
                        peerSession.sendMessage(new TextMessage(json));
                    } catch (IOException e) {
                        log.info("Exception occurred while sending group typing status");
                    }
                }));
    }
}
