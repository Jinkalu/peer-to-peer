package com.peertopeer.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.peertopeer.enums.MessageStatus;
import com.peertopeer.service.PresenceService;
import com.peertopeer.service.StatusService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

import static com.peertopeer.config.handlers.ChatWebSocketHandler.getUserSession;
import static com.peertopeer.utils.PeerUtils.getParam;
import static com.peertopeer.utils.PeerUtils.getPrivateChatId;

@Service
@RequiredArgsConstructor
public class StatusServiceImpl implements StatusService {

    private final PresenceService presenceService;

    @Override
    public void sendStatus(MessageStatus status, String sender,
                           String messageId, String conversationId) throws IOException {
        Map<String, Object> deliveredStatus = Map.of(
                "conversationId", conversationId,
                "statusReceiver", sender,
                "msgId", messageId,
                "type", "status",
                "status", status
        );
        WebSocketSession senderSession = getUserSession(sender);
        System.out.println("STATUS : : " + deliveredStatus);
        if (senderSession != null && senderSession.isOpen()) {
            senderSession.sendMessage(new TextMessage(new ObjectMapper().writeValueAsString(deliveredStatus)));
        }
    }

    private final Map<String, Object> sessionLocks = new ConcurrentHashMap<>();

    @Override
    public void handleTypingStatus(WebSocketSession session, Map<String,
            String> payload) {
//        Object lock = sessionLocks.computeIfAbsent(session.getId(), k -> new Object());
        String fromUser = getParam(session, "sender");
        String toUser = payload.get("to");
        WebSocketSession peerSession = getUserSession(toUser);
        if (Objects.nonNull(peerSession) && peerSession.isOpen()) {
            Object peerLock = sessionLocks.computeIfAbsent(peerSession.getId(), k -> new Object());
            synchronized (peerLock) {
                try {
                    if (peerSession.isOpen()) {
                        Map<String, Object> typingMessage = Map.of(
                                "type", "typing",
                                "from", fromUser,
                                "isTyping", payload.get("isTyping")
                        );
                        String json = new ObjectMapper().writeValueAsString(typingMessage);
                        peerSession.sendMessage(new TextMessage(json));
                    }
                } catch (IllegalStateException e) {
                    System.out.println("WebSocket not ready for sending : " + e.getMessage());
                } catch (Exception e) {
                    System.out.println("Error sending typing status : " + e.getMessage());
                }
            }
        }
    }

    // Clean up locks when session closes
    public void removeSessionLock(String sessionId) {
        sessionLocks.remove(sessionId);
    }

/*    @Override
    public void handleTypingStatus(WebSocketSession session, Map<String,
            String> payload) throws IOException {
        String fromUser = getParam(session, "sender");
        String toUser = payload.get("to");


        String chatId = getPrivateChatId(fromUser, toUser);

        if (Objects.equals(payload.get("isTyping"), "true")) {
            presenceService.setTyping(chatId, fromUser);
        } else {
            presenceService.clearTyping(chatId, fromUser);
        }

        WebSocketSession peerSession = getUserSession(toUser);
        if (peerSession != null && peerSession.isOpen()) {
            Map<String, Object> typingMessage = Map.of(
                    "type", "typing",
                    "from", fromUser,
                    "isTyping", payload.get("isTyping")
            );
            System.out.println("typing status : : " + payload.get("isTyping"));
            String json = new ObjectMapper().writeValueAsString(typingMessage);
            peerSession.sendMessage(new TextMessage(json));
        }
    }*/
}
