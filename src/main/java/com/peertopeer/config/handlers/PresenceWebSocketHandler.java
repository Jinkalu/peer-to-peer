package com.peertopeer.config.handlers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.peertopeer.service.ChatService;
import com.peertopeer.service.ConversationService;
import com.peertopeer.service.PresenceService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import static com.peertopeer.utils.PeerUtils.getParam;

@Slf4j
@Component
public class PresenceWebSocketHandler extends TextWebSocketHandler {

    private final PresenceService presenceService;
    private final ConversationService conversationService;

    public static PresenceWebSocketHandler INSTANCE;

    public PresenceWebSocketHandler(PresenceService presenceService,
                                    ConversationService conversationService) {
        this.presenceService = presenceService;
        this.conversationService = conversationService;
        INSTANCE = this;
    }

    private final Map<String, WebSocketSession> userSessions = new ConcurrentHashMap<>();
    private static final Map<String, Set<WebSocketSession>> subscribers = new ConcurrentHashMap<>();

    @Async
    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        String connectedUser = getParam(session, "user");
        String type = getParam(session, "type");

        if ("ping".equals(type)) {
            if (connectedUser != null) {
                presenceService.markOnline(connectedUser);
                notifyUserStatus(connectedUser, true);
            }
        } else if ("subscribe".equals(type)) {
            String target = getParam(session, "target");
            String convoId = getParam(session, "convoId");
            if (target != null) {
                subscribers.computeIfAbsent(target, k -> ConcurrentHashMap.newKeySet()).add(session);
                Long count = conversationService.unreadCountInConvo(Long.valueOf(connectedUser), Long.valueOf(convoId));
                boolean isOnline = presenceService.isOnline(target);

                Map<String, Object> message = Map.of(
                        "conversationId", convoId,
                        "user", target,
                        "online", isOnline,
                        "unreadCount", count
                );
                String json = new ObjectMapper().writeValueAsString(message);
                session.sendMessage(new TextMessage(json));
            }
        }
        session.getAttributes().put("userId", connectedUser);
    }

    @Async
    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        String disconnectedUser = getUserBySession(session);
        if (disconnectedUser != null) {
            presenceService.markOffline(disconnectedUser);
            notifyUserStatus(disconnectedUser, false);
        }
        subscribers.values().forEach(set -> set.remove(session));
    }


    private String getUserBySession(WebSocketSession session) {
        Object userId = session.getAttributes().get("userId");
        return userId != null ? userId.toString() : null;
    }


    private void notifyUserStatus(String user, boolean isOnline) {
        Set<WebSocketSession> subs = subscribers.getOrDefault(user, Set.of());

        for (WebSocketSession session : subs) {
            try {
                if (session.isOpen()) {
                    synchronized (session) {

                        Map<String, Object> message = Map.of(
                                "user", user,
                                "online", isOnline
                        );
                        String json = new ObjectMapper().writeValueAsString(message);
                        session.sendMessage(new TextMessage(json));
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static WebSocketSession getSubscribed(String userId, String subscribedBy) {
        return subscribers.getOrDefault(userId, Set.of())
                .stream()
                .filter(webSocketSession ->
                        subscribedBy.equals(getParam(webSocketSession, "user")))
                .findFirst().orElse(null);
    }


}
