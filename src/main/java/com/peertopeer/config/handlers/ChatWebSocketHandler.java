package com.peertopeer.config.handlers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.peertopeer.enums.MessageStatus;
import com.peertopeer.service.ChatService;
import com.peertopeer.service.PresenceService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import org.thymeleaf.util.StringUtils;

import java.io.IOException;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import static com.peertopeer.utils.PeerUtils.getMessageStatus;
import static com.peertopeer.utils.PeerUtils.getParam;

@Component
@RequiredArgsConstructor
public class ChatWebSocketHandler extends TextWebSocketHandler {

    private final Map<String, Set<WebSocketSession>> roomSessions = new ConcurrentHashMap<>();

    private final Map<String, WebSocketSession> userSessions = new ConcurrentHashMap<>();

//    private final Map<String,>

    private final PresenceService presenceService;

    private final ChatService chatService;


    @Async
    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws IOException {
        String user = getParam(session, "sender");
        userSessions.put(user, session);

        String type = getParam(session, "type");
        if ("group".equals(type)) {
            String room = getParam(session, "target");
            roomSessions.computeIfAbsent(room, r -> ConcurrentHashMap.newKeySet()).add(session);
            session.getAttributes().put("room", room);
        }

        if ("private".equals(type)) {
            String target = getParam(session, "receiver");
            if (!StringUtils.isEmpty(target)) {
                Long conversationId = chatService.create(user, target);
                session.sendMessage(new TextMessage("conversationId:" + conversationId));
            }
        }

        session.getAttributes().put("user", user);
        session.getAttributes().put("type", type);
    }

    @Async
    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        Map<String, String> payload = new ObjectMapper().readValue(message.getPayload(), Map.class);
        String type = (String) session.getAttributes().get("type");


        if ("typing".equals(payload.get("type"))) {
            handleTypingStatus(session, payload);
            return;
        }

        if ("group".equals(type)) {
            groupMsg(session, payload);
        } else if ("private".equals(type)) {
            privateMsg(session, payload);
        }
    }



    @Async
    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        String user = (String) session.getAttributes().get("user");
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

    private void handleTypingStatus(WebSocketSession session, Map<String, String> payload) throws IOException {
        String fromUser = (String) session.getAttributes().get("user");
        String toUser = payload.get("to");


        String chatId = getPrivateChatId(fromUser, toUser);

        if (Objects.equals(payload.get("isTyping"), "true")) {
            presenceService.setTyping(chatId, fromUser);
        } else {
            presenceService.clearTyping(chatId, fromUser);
        }


        WebSocketSession peerSession = userSessions.get(toUser);
        if (peerSession != null && peerSession.isOpen()) {
            Map<String, Object> typingMessage = Map.of(
                    "type", "typing",
                    "from", fromUser,
                    "isTyping", payload.get("isTyping")
            );
            String json = new ObjectMapper().writeValueAsString(typingMessage);
            peerSession.sendMessage(new TextMessage(json));
        }
    }

    private String getPrivateChatId(String user1, String user2) {
        return user1.compareTo(user2) < 0
                ? user1 + "_" + user2
                : user2 + "_" + user1;
    }


    private void groupMsg(WebSocketSession session, Map<String, String> payload) throws IOException {
        String room = (String) session.getAttributes().get("room");
        String user = getParam(session, "user");
        String msg = payload.get("msg");
        for (WebSocketSession s : roomSessions.getOrDefault(room, Set.of())) {
            if (s.isOpen()) {
                s.sendMessage(new TextMessage(String.format("{\"from\":\"%s\", \"msg\":%s}",
                        user, msg)));
            }
        }
    }


    private void privateMsg(WebSocketSession session, Map<String, String> payload) throws IOException {

        String receiver = payload.get("receiver");
        String sender = getParam(session, "sender");
        String conversationId = getParam(session, "conversationId");

        WebSocketSession peerSession = getUserSession(receiver);

        boolean isOnScreen = peerSession != null && peerSession.isOpen();
        boolean online = presenceService.isOnline(receiver);

        MessageStatus status = getMessageStatus(online, isOnScreen);

        String msg = payload.get("msg");
        String messageId = String.valueOf(chatService.saveMessage(conversationId, sender, msg, status));

        if (online && isOnScreen) {
            Map<String, String> response = Map.of(
                    "conversationId", conversationId,
                    "sender", sender,
                    "receiver", receiver,
                    "msg", msg,
                    "msgId", messageId
            );
            peerSession.sendMessage(new TextMessage(new ObjectMapper().writeValueAsString(response)));
        }
        // ✅ Send delivered status to sender
        sendStatus(status, sender, messageId, conversationId);
    }


    private void sendStatus(MessageStatus status, String fromUser, String msgId,
                            String conversationId) throws IOException {
        Map<String, Object> deliveredStatus = Map.of(
                "conversationId", conversationId,
                "statusReceiver", fromUser,
                "msgId", msgId,
                "type", "status",
                "status", status
        );
        WebSocketSession senderSession = getUserSession(fromUser);
        if (senderSession != null && senderSession.isOpen()) {
            senderSession.sendMessage(new TextMessage(new ObjectMapper().writeValueAsString(deliveredStatus)));
        }
    }


    public WebSocketSession getUserSession(String user) {
        return userSessions.get(user);
    }
}

