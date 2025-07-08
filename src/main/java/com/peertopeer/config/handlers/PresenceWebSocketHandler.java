package com.peertopeer.config.handlers;

import com.peertopeer.service.PresenceService;
import org.springframework.scheduling.annotation.Async;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import static com.peertopeer.utils.PeerUtils.getParam;

public class PresenceWebSocketHandler extends TextWebSocketHandler {

    private final PresenceService presenceService;

    public static PresenceWebSocketHandler INSTANCE;

    public PresenceWebSocketHandler(PresenceService presenceService) {
        this.presenceService = presenceService;
        INSTANCE = this;
    }

    private final Map<String, WebSocketSession> userSessions = new ConcurrentHashMap<>();
    private final Map<String, Set<WebSocketSession>> subscribers = new ConcurrentHashMap<>();

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
            if (target != null) {
                subscribers.computeIfAbsent(target, k -> ConcurrentHashMap.newKeySet()).add(session);

                boolean isOnline = presenceService.isOnline(target);
                session.sendMessage(new TextMessage(
                        String.format("{\"user\":\"%s\", \"online\":%s}", target, isOnline)
                ));
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
        String json = String.format("{\"user\":\"%s\", \"online\":%s}", user, isOnline);
        TextMessage msg = new TextMessage(json);

        for (WebSocketSession session : subs) {
            try {
                if (session.isOpen()) {
                    synchronized (session) {
                        session.sendMessage(msg);
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }



}
