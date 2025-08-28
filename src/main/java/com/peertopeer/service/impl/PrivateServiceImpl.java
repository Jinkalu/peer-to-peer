package com.peertopeer.service.impl;

import com.peertopeer.service.*;
import lombok.RequiredArgsConstructor;
import com.peertopeer.enums.MessageStatus;
import com.peertopeer.vo.MessageResponseVO;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.TextMessage;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.web.socket.WebSocketSession;

import java.util.Map;
import java.util.Objects;
import java.io.IOException;

import static com.peertopeer.utils.PeerUtils.*;
import static com.peertopeer.socket.handlers.PresenceWebSocketHandler.getSubscribed;
import static com.peertopeer.socket.handlers.PrivateChatWebSocketHandler.getUserSession;

@Service
@RequiredArgsConstructor
public class PrivateServiceImpl implements PrivateChat {

    private final ChatService chatService;
    private final StatusService statusService;
    private final PresenceService presenceService;
    private final ConversationService conversationService;

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
        String sender = (String) session.getAttributes().get("userId");
        String conversationId = getParam(session, "conversationId");
        String receiver = conversationService.findPeerUser(Long.valueOf(conversationId), Long.valueOf(sender));

        WebSocketSession peerSession = getUserSession(receiver);

        boolean isOnScreen = presenceService.isOnScreen(receiver, conversationId);
        boolean online = presenceService.isOnline(receiver);

        MessageStatus status = getMessageStatus(online, isOnScreen);

        String msg = payload.get("msg");
        String messageId = payload.get("messageId");
        String replayTo = payload.get("replayTo");
        chatService.saveMessage(conversationId, messageId, sender, msg, replayTo, status);

        if (online && isOnScreen) {
            MessageResponseVO response = MessageResponseVO.builder()
                    .messageId(messageId)
                    .msg(msg)
                    .conversationId(conversationId)
                    .sender(sender)
                    .receiver(receiver)
                    .build();
            peerSession.sendMessage(new TextMessage(new ObjectMapper().writeValueAsString(response)));
        }
        messageNotification(sender, receiver, conversationId);
        statusService.sendStatus(status, sender, messageId, conversationId);
    }

    @Override
    public Long findPeerUser(Long conversationId, Long sender) {

        return 0L;
    }

    private void messageNotification(String sender, String receiver, String conversationId) throws IOException {
        WebSocketSession subscribed = getSubscribed(sender, receiver);
        if (Objects.nonNull(subscribed) && subscribed.isOpen()) {
            MessageResponseVO response = MessageResponseVO.builder()
                    .user(sender)
                    .online(true)
                    .unreadCount(conversationService.unreadCountInConvo(Long.valueOf(sender),
                            Long.valueOf(conversationId)))
                    .conversationId(conversationId)
                    .build();
            subscribed.sendMessage(new TextMessage(new ObjectMapper().writeValueAsString(response)));
        }
    }

}
