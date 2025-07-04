package com.peertopeer.config.handlers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.peertopeer.service.PresenceService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import static com.peertopeer.utils.PeerUtils.getParam;

@Component
@RequiredArgsConstructor
public class OnScreenPresenceWebSocketHandler extends TextWebSocketHandler {

    private final PresenceService presenceService;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final Map<String, Set<WebSocketSession>> chatIdSubscribers = new ConcurrentHashMap<>();
    private final Map<String, WebSocketSession> sessionMap = new ConcurrentHashMap<>();
    private final Map<String, String> sessionToUserChat = new ConcurrentHashMap<>();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        String user = getParam(session, "user");
        String chatId = getParam(session, "chatId");

        if (user == null || chatId == null) {
            session.close(CloseStatus.BAD_DATA);
            return;
        }

        String sessionKey = user + "_" + chatId;
        sessionMap.put(sessionKey, session);
        sessionToUserChat.put(session.getId(), sessionKey);
        chatIdSubscribers.computeIfAbsent(chatId, k -> ConcurrentHashMap.newKeySet()).add(session);
        session.getAttributes().put("user", user);
        session.getAttributes().put("chatId", chatId);

        notifyChatSubscribers(chatId, user, true);
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        String sessionKey = sessionToUserChat.remove(session.getId());
        if (sessionKey != null) {
            sessionMap.remove(sessionKey);
            String[] parts = sessionKey.split("_", 2);
            if (parts.length >= 2) {
                String user = parts[0];
                String chatId = parts[1];
                notifyChatSubscribers(chatId, user, false);
            }
        }
        chatIdSubscribers.values().forEach(set -> set.remove(session));
        chatIdSubscribers.entrySet().removeIf(entry -> entry.getValue().isEmpty());
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        Map<String, Object> payload = objectMapper.readValue(message.getPayload(), Map.class);
        String type = (String) payload.get("type");

        switch (type) {
            case "heartbeat" -> handleHeartbeat(session);
            case "getUsersInChat" -> handleGetUsersInChat(session, payload);
            default -> System.out.println("Unknown message type: " + type);
        }
    }

    private void handleHeartbeat(WebSocketSession session) throws IOException {
        Map<String, Object> response = Map.of(
                "type", "heartbeat",
                "timestamp", System.currentTimeMillis()
        );
        session.sendMessage(new TextMessage(objectMapper.writeValueAsString(response)));
    }

    private void handleGetUsersInChat(WebSocketSession session, Map<String, Object> payload) throws IOException {
        String chatId = (String) payload.get("chatId");
        if (chatId == null) return;

        Set<String> usersInChat = getUsersInChat(chatId);
        Map<String, Object> response = Map.of(
                "type", "usersInChat",
                "chatId", chatId,
                "users", usersInChat
        );
        session.sendMessage(new TextMessage(objectMapper.writeValueAsString(response)));
    }

    private void notifyChatSubscribers(String chatId, String fromUser, boolean isPresent) {
        Set<WebSocketSession> subscribers = chatIdSubscribers.getOrDefault(chatId, Set.of());

        Map<String, Object> notification = Map.of(
                "type", "onScreenStatus",
                "from", fromUser,
                "chatId", chatId,
                "present", isPresent,
                "timestamp", System.currentTimeMillis()
        );

        try {
            String payload = objectMapper.writeValueAsString(notification);
            TextMessage textMessage = new TextMessage(payload);
            subscribers.forEach(session -> {
                try {
                    if (session.isOpen()) {
                        String sessionUser = (String) session.getAttributes().get("user");
                        if (!fromUser.equals(sessionUser)) {
                            session.sendMessage(textMessage);
                        }
                    }
                } catch (IOException e) {
                    subscribers.remove(session);
                }
            });
        } catch (IOException e) {
            System.err.println("Error serializing presence notification: " + e.getMessage());
        }
    }

    public Set<String> getUsersInChat(String chatId) {
        return sessionMap.entrySet().stream()
                .filter(entry -> entry.getKey().endsWith("_" + chatId))
                .filter(entry -> entry.getValue().isOpen())
                .map(entry -> entry.getKey().split("_")[0])
                .collect(java.util.stream.Collectors.toSet());
    }
}
