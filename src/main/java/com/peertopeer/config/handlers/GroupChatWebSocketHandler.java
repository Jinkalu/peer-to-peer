package com.peertopeer.config.handlers;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import lombok.RequiredArgsConstructor;
import com.peertopeer.service.StatusService;
import com.peertopeer.service.PresenceService;
import com.peertopeer.service.GroupChatService;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import static com.peertopeer.utils.PeerUtils.getParam;
import static com.peertopeer.utils.PeerUtils.isEmpty;

@Slf4j
@Component
@RequiredArgsConstructor
public class GroupChatWebSocketHandler extends TextWebSocketHandler {

    private final StatusService statusService;
    private final PresenceService presenceService;
    private final GroupChatService groupChatService;

    public static final Map<String, Set<WebSocketSession>> roomSessions = new ConcurrentHashMap<>();
    public static final Map<Long, Set<Long>> activeRoomMembers = new ConcurrentHashMap<>();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        String user = getParam(session, "sender");
        if (isEmpty(user)) {
            session.close(CloseStatus.BAD_DATA);
            return;
        }
        String conversationId = groupChatService.getGroupId(session);
        if (conversationId != null) {
            presenceService.setOnScreen(conversationId, user);
            log.info("User : : {} Connected to group ", user);
            /*
             * set seen status to the all received msg
             */
        }
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        Map<String, String> payload = new ObjectMapper().readValue(message.getPayload(), Map.class);

        if ("typing".equals(payload.get("type"))) {
            statusService.handleGroupTypingStatus(session, payload);
            return;
        }
        if ("msg".equals(payload.get("type"))) {
            groupChatService.groupMsg(session, payload);
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        groupChatService.removeUserFromRoom(session);
    }


/*    private void reloadMessages(String conversationId, String receiver) throws IOException {

        if (presenceService.isOnline(receiver) && presenceService.isOnScreen(receiver, conversationId)) {
            WebSocketSession peerSession = getUserSession(receiver);
            Map<String, String> response = Map.of(
                    "type", "reload",
                    "conversationId", conversationId,
                    "receiver", receiver
            );
            peerSession.sendMessage(new TextMessage(new ObjectMapper().writeValueAsString(response)));
        }
    }*/
}
