package com.peertopeer.socket.handlers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.peertopeer.service.PresenceService;
import com.peertopeer.service.PrivateChat;
import com.peertopeer.service.StatusService;
import com.peertopeer.vo.MessageResponseVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
@RequiredArgsConstructor
public class PrivateChatWebSocketHandler extends BaseAuthenticatedWebSocketHandler {

    public static final Map<String, WebSocketSession> userSessions = new ConcurrentHashMap<>();

    private final PrivateChat privateChat;
    private final StatusService statusService;
    private final PresenceService presenceService;

    @Override
    protected void onAuthenticatedConnection(WebSocketSession session, String username, String userId) throws Exception {
        userSessions.put(userId, session);
        session.getAttributes().put("user", userId);

        String conversationId = privateChat.privateConnect(session, userId);
        presenceService.setOnScreen(conversationId, userId);

        presenceService.getOnScreenUsers(conversationId).stream()
                .filter(u -> !u.equals(userId))
                .findFirst().ifPresent(receiver -> {
                    try {
                        reloadMessages(conversationId, receiver);
                    } catch (IOException e) {
                        log.error("Error reloading messages", e);
                    }
                });
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        if (!isAuthenticated(session)) {
            log.warn("Unauthenticated session attempting to send message");
            session.close(CloseStatus.NOT_ACCEPTABLE.withReason("Authentication required"));
            return;
        }

        Map<String, String> payload = new ObjectMapper().readValue(message.getPayload(), Map.class);

        if ("typing".equals(payload.get("type"))) {
            statusService.handleTypingStatus(session, payload);
            return;
        }

        if ("msg".equals(payload.get("type"))) {
            privateChat.privateMsg(session, payload);
        }
    }

    @Async
    @Override
    protected void onAuthenticatedDisconnection(WebSocketSession session, String username, String userId, CloseStatus status) throws Exception {
        String conversationId = session.getAttributes().get("conversationId").toString();
        presenceService.offScreen(userId, conversationId);
        userSessions.remove(userId);

        Set<String> onScreenUsers = presenceService.getOnScreenUsers(conversationId);
        if (!CollectionUtils.isEmpty(onScreenUsers)) {
            onScreenUsers.stream()
                    .filter(u -> !u.equals(userId))
                    .findFirst().ifPresent(receiver -> {
                        try {
                            WebSocketSession peerSession = getUserSession(receiver);
                            if (peerSession != null && peerSession.isOpen()) {
                                peerSession.sendMessage(new TextMessage(
                                        new ObjectMapper().writeValueAsString(Map.of("type", "receiverStatus"))));
                            }
                        } catch (IOException e) {
                            log.error("Error sending receiver status", e);
                        }
                    });
        }
    }

    private void reloadMessages(String conversationId, String receiver) throws IOException {
        if (presenceService.isOnline(receiver) && presenceService.isOnScreen(receiver, conversationId)) {
            WebSocketSession peerSession = getUserSession(receiver);
            if (peerSession != null && peerSession.isOpen()) {
                MessageResponseVO response = MessageResponseVO.builder()
                        .type("reload")
                        .conversationId(conversationId)
                        .receiver(receiver)
                        .build();
                peerSession.sendMessage(new TextMessage(new ObjectMapper().writeValueAsString(response)));
            }
        }
    }

    public static WebSocketSession getUserSession(String user) {
        return userSessions.get(user);
    }
}