package com.peertopeer.service;

import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.util.Map;

public interface PrivateChat {
    String privateConnect(WebSocketSession session, String user) throws IOException;

    void privateMsg(WebSocketSession session, Map<String, String> payload) throws IOException;

    Long findPeerUser(Long conversationId, Long sender);
}
