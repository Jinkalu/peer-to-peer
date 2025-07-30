package com.peertopeer.socket.handlers;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.peertopeer.service.ConversationService;
import com.peertopeer.service.PresenceService;
import com.peertopeer.utils.PeerUtils;
import com.peertopeer.vo.MessageResponseVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
public class PresenceWebSocketHandler extends BaseAuthenticatedWebSocketHandler {

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
    protected void onAuthenticatedConnection(WebSocketSession session, String username, String userId) throws Exception {
        String type = PeerUtils.getParam(session, "type");
        if ("ping".equals(type)) {
            presenceService.markOnline(userId);
            notifyUserStatus(userId, true);
        } else if ("subscribe".equals(type)) {
            String target = PeerUtils.getParam(session, "target");
            String convoId = PeerUtils.getParam(session, "convoId");
            if (target != null) {
                subscribers.computeIfAbsent(target, k -> ConcurrentHashMap.newKeySet()).add(session);
                Long count = conversationService.unreadCountInConvo(Long.valueOf(userId), Long.valueOf(convoId));
                boolean isOnline = presenceService.isOnline(target);
                MessageResponseVO message = MessageResponseVO.builder()
                        .conversationId(convoId)
                        .user(target)
                        .online(isOnline)
                        .unreadCount(count)
                        .build();
                ObjectMapper mapper = new ObjectMapper();
                mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
                String json = mapper.writeValueAsString(message);
                session.sendMessage(new TextMessage(json));
            }
        }

        userSessions.put(userId, session);
        session.getAttributes().put("userId", userId);
    }

    @Async
    @Override
    protected void onAuthenticatedDisconnection(WebSocketSession session, String username, String userId, CloseStatus status) throws Exception {
        presenceService.markOffline(userId);
        notifyUserStatus(userId, false);
        subscribers.values().forEach(set -> set.remove(session));
        userSessions.remove(userId);
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
                        MessageResponseVO message = MessageResponseVO.builder()
                                .user(user)
                                .online(isOnline)
                                .build();
                        String json = new ObjectMapper().writeValueAsString(message);
                        session.sendMessage(new TextMessage(json));
                    }
                }
            } catch (IOException e) {
                log.error("Error sending presence notification", e);
            }
        }
    }

    public static WebSocketSession getSubscribed(String userId, String subscribedBy) {
        return subscribers.getOrDefault(userId, Set.of())
                .stream()
                .filter(webSocketSession -> {
                    String sessionUserId = (String) webSocketSession.getAttributes().get("userId");
                    return subscribedBy.equals(sessionUserId);
                })
                .findFirst().orElse(null);
    }
}