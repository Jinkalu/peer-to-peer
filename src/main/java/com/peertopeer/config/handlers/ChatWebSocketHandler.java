package com.peertopeer.config.handlers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.peertopeer.enums.MessageStatus;
import com.peertopeer.service.ChatService;
import com.peertopeer.service.PresenceService;
import com.peertopeer.service.PrivateChat;
import com.peertopeer.service.StatusService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import static com.peertopeer.utils.PeerUtils.*;

@Component
@RequiredArgsConstructor
public class ChatWebSocketHandler extends TextWebSocketHandler {

    private final Map<String, Set<WebSocketSession>> roomSessions = new ConcurrentHashMap<>();

    public static final Map<String, WebSocketSession> userSessions = new ConcurrentHashMap<>();

    private final PresenceService presenceService;
    private final StatusService statusService;
    private final PrivateChat privateChat;


    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws IOException {
        String user = getParam(session, "sender");
        if (isEmpty(user)) {
            session.close(CloseStatus.BAD_DATA);
            return;
        }
        userSessions.put(user, session);
        session.getAttributes().put("user", user);

        String type = getParam(session, "type");
        String conversationId;

        if ("group".equalsIgnoreCase(type)) {
            conversationId = getParam(session, "conversationId");
            if (!isEmpty(conversationId)) {
                roomSessions.computeIfAbsent(conversationId,
                        r -> ConcurrentHashMap.newKeySet()).add(session);
                session.getAttributes().put("room", conversationId);
            }
        } else if ("private".equalsIgnoreCase(type)) {
            conversationId = privateChat.privateConnect(session, user);
        } else {
            conversationId = null;
        }

        // Mark the user as "on-screen" after setting the conversation ID
        if (conversationId != null) {
            presenceService.setOnScreen(conversationId, user);
            presenceService.getOnScreenUsers(conversationId).stream().
                    filter(u -> !u.equals(user))
                    .findFirst().ifPresent(receiver -> {
                        try {
                            reloadMessages(conversationId, receiver);
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    });
        }
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        Map<String, String> payload = new ObjectMapper().readValue(message.getPayload(), Map.class);
        String type = getParam(session, "type");


        if ("typing".equals(payload.get("type"))) {
            statusService.handleTypingStatus(session, payload);
            return;
        }

        if ("group".equals(type)) {
            groupMsg(session, payload);
        } else if ("private".equals(type)) {
            privateChat.privateMsg(session, payload);
        }
    }

    @Async
    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        String user = (String) session.getAttributes().get("user");
        String conversationId = session.getAttributes().get("conversationId").toString();
        presenceService.offScreen(user, conversationId);
        userSessions.remove(user);

        String room = (String) session.getAttributes().get("room");
        if (room != null) {
            Set<WebSocketSession> sessions = roomSessions.get(room);
            if (sessions != null) {
                sessions.remove(session);
                if (sessions.isEmpty()) roomSessions.remove(room);
            }
        }
    }


    private void reloadMessages(String conversationId, String receiver) throws IOException {

        if (presenceService.isOnline(receiver) && presenceService.isOnScreen(receiver, conversationId)) {
            WebSocketSession peerSession = getUserSession(receiver);
            Map<String, String> response = Map.of(
                    "type", "reload",
                    "conversationId", conversationId,
                    "receiver", receiver
            );
            peerSession.sendMessage(new TextMessage(new ObjectMapper().writeValueAsString(response)));
        }
    }


    private void groupMsg(WebSocketSession session, Map<String, String> payload) {
        String room = getParam(session, "conversationId");
        String user = getParam(session, "sender");
        String msg = payload.get("msg");
        roomSessions.getOrDefault(room, Collections.emptySet()).stream()
                .filter(peerSession -> peerSession.isOpen() && !peerSession.getAttributes().get("user").equals(user))
                .forEach(peerSession -> {
                    try {
                        String message = String.format("{\"from\":\"%s\", \"msg\":\"%s\"}", user, msg);
                        peerSession.sendMessage(new TextMessage(message));
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                });
    }


    public static WebSocketSession getUserSession(String user) {
        return userSessions.get(user);
    }
}

