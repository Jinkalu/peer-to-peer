package com.peertopeer.config.handlers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.peertopeer.service.PresenceService;
import com.peertopeer.service.PrivateChat;
import com.peertopeer.service.StatusService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import static com.peertopeer.utils.PeerUtils.getParam;
import static com.peertopeer.utils.PeerUtils.isEmpty;

@Component
@RequiredArgsConstructor
public class PrivateChatWebSocketHandler extends TextWebSocketHandler {


    public static final Map<String, WebSocketSession> userSessions = new ConcurrentHashMap<>();

    private final PrivateChat privateChat;
    private final StatusService statusService;
    private final PresenceService presenceService;


    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws IOException {
        String currentUser = getParam(session, "sender");
        if (isEmpty(currentUser)) {
            session.close(CloseStatus.BAD_DATA);
            return;
        }
        userSessions.put(currentUser, session);
        session.getAttributes().put("user", currentUser);

        String conversationId = privateChat.privateConnect(session, currentUser);
        presenceService.setOnScreen(conversationId, currentUser);

        presenceService.getOnScreenUsers(conversationId).stream().
                filter(u -> !u.equals(currentUser))
                .findFirst().ifPresent(receiver -> {
                    try {
                        reloadMessages(conversationId, receiver);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                });
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
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
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        String currentUser = (String) session.getAttributes().get("user");
        String conversationId = session.getAttributes().get("conversationId").toString();
        presenceService.offScreen(currentUser, conversationId);
        userSessions.remove(currentUser);
        Set<String> onScreenUsers = presenceService.getOnScreenUsers(conversationId);
        if (!CollectionUtils.isEmpty(onScreenUsers)) {
            onScreenUsers.stream().
                    filter(u -> !u.equals(currentUser))
                    .findFirst().ifPresent(receiver -> {
                        try {
                            WebSocketSession peerSection = getUserSession(receiver);
                            peerSection.sendMessage(new TextMessage(
                                    new ObjectMapper().writeValueAsString(Map.of("type", "receiverStatus"))));
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    });
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


    public static WebSocketSession getUserSession(String user) {
        return userSessions.get(user);
    }
}

