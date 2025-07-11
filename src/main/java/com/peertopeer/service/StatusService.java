package com.peertopeer.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.peertopeer.enums.MessageStatus;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.util.Map;

public interface StatusService {
    void sendStatus(MessageStatus status, String sender, String messageId, String conversationId) throws IOException;

    void handleTypingStatus(WebSocketSession session, Map<String, String> payload) throws IOException;
}
