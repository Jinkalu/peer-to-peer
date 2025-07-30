package com.peertopeer.socket.handlers;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

@Slf4j
public abstract class BaseAuthenticatedWebSocketHandler extends TextWebSocketHandler {

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {

        if (!isAuthenticated(session)) {
            log.warn("Unauthenticated connection attempt");
            session.close(CloseStatus.NOT_ACCEPTABLE.withReason("Authentication required"));
            return;
        }

        String username = getUsername(session);
        String userId = getUserId(session);

        log.info("WebSocket connection established for user: {} (ID: {})", username, userId);
        onAuthenticatedConnection(session, username, userId);
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        if (!isAuthenticated(session)) {
            return;
        }

        String username = getUsername(session);
        String userId = getUserId(session);

        log.info("WebSocket connection closed for user: {} (ID: {})", username, userId);

        onAuthenticatedDisconnection(session, username, userId, status);
    }

    protected String getUsername(WebSocketSession session) {
        return (String) session.getAttributes().get("username");
    }

    protected String getUserId(WebSocketSession session) {
        return (String) session.getAttributes().get("userId");
    }

    protected String getToken(WebSocketSession session) {
        return (String) session.getAttributes().get("token");
    }

    protected boolean isAuthenticated(WebSocketSession session) {
        return getUsername(session) != null && getUserId(session) != null;
    }

    protected String getParam(WebSocketSession session, String key) {
        Object value = session.getAttributes().get(key);
        return value != null ? value.toString() : null;
    }

    protected boolean isEmpty(String str) {
        return str == null || str.trim().isEmpty();
    }

    protected abstract void onAuthenticatedConnection(WebSocketSession session,
                                                      String username, String userId) throws Exception;

    protected abstract void onAuthenticatedDisconnection(WebSocketSession session,
                                                         String username, String userId,
                                                         CloseStatus status) throws Exception;
}