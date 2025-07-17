package com.peertopeer.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.peertopeer.config.handlers.PresenceWebSocketHandler;
import com.peertopeer.enums.MessageStatus;
import com.peertopeer.service.ChatService;
import com.peertopeer.service.PresenceService;
import com.peertopeer.service.PrivateChat;
import com.peertopeer.service.StatusService;
import jakarta.websocket.OnClose;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.util.Map;
import java.util.Objects;

import static com.peertopeer.config.handlers.ChatWebSocketHandler.getUserSession;
import static com.peertopeer.config.handlers.PresenceWebSocketHandler.getSubscribed;
import static com.peertopeer.utils.PeerUtils.*;

@Service
@RequiredArgsConstructor
public class PrivateServiceImpl implements PrivateChat {

    private final ChatService chatService;
    private final PresenceService presenceService;
    private final StatusService statusService;

    @Override
    public String privateConnect(WebSocketSession session, String user) throws IOException {
        String conversationId;
        String receiver = getParam(session, "receiver");
        if (!isEmpty(receiver)) {
            conversationId = String.valueOf(chatService.create(user, receiver));
            chatService.updateMessageChatStatus(Long.parseLong(conversationId), user);
            session.getAttributes().put("conversationId", conversationId);
            session.sendMessage(new TextMessage("conversationId:" + conversationId));
        } else {
            String cid = getParam(session, "conversationId");
            if (!isEmpty(cid)) {
                conversationId = cid;
                long value = Long.parseLong(cid);
                chatService.updateMessageChatStatus(value, user);
                session.getAttributes().put("conversationId", value);
            } else {
                conversationId = null;
            }
        }
        return conversationId;
    }


    @Override
    public void privateMsg(WebSocketSession session, Map<String, String> payload) throws IOException {
        String receiver = payload.get("receiver");
        String sender = getParam(session, "sender");
        String conversationId = getParam(session, "conversationId");

        WebSocketSession peerSession = getUserSession(receiver);

        boolean isOnScreen = presenceService.isOnScreen(receiver, conversationId);
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
        messageNotification(sender, receiver);
        // ✅ Send delivered status to sender
        statusService.sendStatus(status, sender, messageId, conversationId);
    }

    private void messageNotification(String sender, String receiver) throws IOException {
        WebSocketSession subscribed = getSubscribed(sender, receiver);
        if (Objects.nonNull(subscribed) && subscribed.isOpen()) {
            subscribed.sendMessage(new TextMessage(
                    String.format("{\"user\":\"%s\", \"online\":%s,\"unreadCount\":%s}", sender, true, chatService.unreadCount(sender, receiver))
            ));
        }
    }

}
