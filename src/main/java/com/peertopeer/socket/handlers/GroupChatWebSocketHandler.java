package com.peertopeer.socket.handlers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.peertopeer.service.GroupChatService;
import com.peertopeer.service.PresenceService;
import com.peertopeer.service.StatusService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
@RequiredArgsConstructor
public class GroupChatWebSocketHandler extends BaseAuthenticatedWebSocketHandler {

    private final StatusService statusService;
    private final PresenceService presenceService;
    private final GroupChatService groupChatService;

    public static final Map<String, Set<WebSocketSession>> roomSessions = new ConcurrentHashMap<>();
    public static final Map<Long, Set<Long>> activeRoomMembers = new ConcurrentHashMap<>();

    @Override
    protected void onAuthenticatedConnection(WebSocketSession session, String username, String userId) {
        String conversationId = groupChatService.getGroupId(session);
        if (conversationId != null) {
            presenceService.setOnScreen(conversationId, userId);
            log.info("User: {} Connected to group", username);

            session.getAttributes().put("sender", userId);
        }
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        if (!isAuthenticated(session)) {
            log.warn("Unauthenticated session attempting to send message");
            session.close(CloseStatus.NOT_ACCEPTABLE.withReason("Authentication required"));
            return;
        }

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
    protected void onAuthenticatedDisconnection(WebSocketSession session, String username,
                                                String userId, CloseStatus status) {
        groupChatService.removeUserFromRoom(session);
    }
}