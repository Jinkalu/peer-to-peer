package com.peertopeer.service.impl;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.peertopeer.enums.MessageStatus;
import com.peertopeer.service.ChatService;
import com.peertopeer.service.GroupChatService;
import com.peertopeer.vo.MessageResponseVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

import static com.peertopeer.socket.handlers.GroupChatWebSocketHandler.roomSessions;
import static com.peertopeer.utils.PeerUtils.getParam;

@Slf4j
@RequiredArgsConstructor
public abstract class GroupMessagingHelper  implements GroupChatService {

    // Use a single ObjectMapper instance (thread-safe)
    private final ObjectMapper objectMapper = new ObjectMapper();

    private final ChatService chatService;

    // Dedicated thread pool for broadcasting
    private final ExecutorService broadcastExecutor =
            Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());

    // Configuration
    private static final int MAX_BATCH_SIZE = 100;
    private static final int BATCH_DELAY_MS = 5;

    @Override
    public void groupMsg(WebSocketSession session, Map<String, String> payload) {
        String room = getParam(session, "conversationId");
        String user = (String) session.getAttributes().get("userId");
        String msg = payload.get("msg");
        String replayTo = payload.get("replayTo");

        // Save message to database
        chatService.saveMessage(room, user, msg, replayTo, MessageStatus.DELIVERED);

        // Broadcast optimized
        broadcastMessageOptimized(room, user, msg, session);
    }

    private void broadcastMessageOptimized(String room, String user, String msg, WebSocketSession session) {
        // 1. Build and serialize message ONCE
        String json;
        try {
            MessageResponseVO message = MessageResponseVO.builder()
                    .type("msg")
                    .sender(user)
                    .senderUsername((String) session.getAttributes().get("username"))
                    .msg(msg)
                    .build();
            json = objectMapper.writeValueAsString(message);
        } catch (Exception e) {
            log.error("Failed to serialize message for room {}: {}", room, e.getMessage());
            return;
        }

        // 2. Get all valid sessions in one pass
        List<WebSocketSession> targetSessions = roomSessions
                .getOrDefault(room, Collections.emptySet())
                .stream()
                .filter(peerSession -> peerSession.isOpen()
                        && !Objects.equals(peerSession.getAttributes().get("userId"), user))
                .collect(Collectors.toList());

        if (targetSessions.isEmpty()) {
            log.debug("No active sessions found for room {}", room);
            return;
        }

        // 3. Send using single async task
        TextMessage textMessage = new TextMessage(json);
        CompletableFuture.runAsync(() -> {
            sendToAllSessions(targetSessions, textMessage, room);
        }, broadcastExecutor);

        log.debug("Broadcasting message to {} users in room {}", targetSessions.size(), room);
    }

    private void sendToAllSessions(List<WebSocketSession> sessions, TextMessage message, String room) {
        int successCount = 0;
        int failureCount = 0;

        for (WebSocketSession peerSession : sessions) {
            try {
                if (peerSession.isOpen()) {
                    // Synchronize per session to avoid concurrent send issues
                    synchronized (peerSession) {
                        peerSession.sendMessage(message);
                    }
                    successCount++;
                } else {
                    failureCount++;
                }
            } catch (Exception e) {
                failureCount++;
                log.debug("Failed to send message to session in room {}: {}", room, e.getMessage());
                // Optionally remove dead session
                removeDeadSession(room, peerSession);
            }
        }

        log.debug("Message broadcast completed for room {}: {} sent, {} failed",
                room, successCount, failureCount);
    }

    // Helper method to clean up dead sessions
    private void removeDeadSession(String room, WebSocketSession deadSession) {
        Set<WebSocketSession> sessions = roomSessions.get(room);
        if (sessions != null) {
            sessions.remove(deadSession);
            if (sessions.isEmpty()) {
                roomSessions.remove(room);
            }
        }
    }

/*    // OPTION 2: Batched Version (For very large groups 500+)
    public void groupMsgBatched(WebSocketSession session, Map<String, String> payload) {
        String room = getParam(session, "conversationId");
        String user = (String) session.getAttributes().get("userId");
        String msg = payload.get("msg");

        chatService.saveMessage(room, user, msg, MessageStatus.DELIVERED);
        broadcastMessageBatched(room, user, msg, session);
    }

    private void broadcastMessageBatched(String room, String user, String msg, WebSocketSession session) {
        // Serialize once
        String json;
        try {
            MessageResponseVO message = MessageResponseVO.builder()
                    .type("msg")
                    .sender(user)
                    .senderUsername((String) session.getAttributes().get("username"))
                    .msg(msg)
                    .build();
            json = objectMapper.writeValueAsString(message);
        } catch (Exception e) {
            log.error("Failed to serialize message for room {}: {}", room, e.getMessage());
            return;
        }

        List<WebSocketSession> targetSessions = roomSessions
                .getOrDefault(room, Collections.emptySet())
                .stream()
                .filter(peerSession -> peerSession.isOpen()
                        && !Objects.equals(peerSession.getAttributes().get("userId"), user))
                .collect(Collectors.toList());

        if (targetSessions.isEmpty()) return;

        // Process in batches for very large groups
        TextMessage textMessage = new TextMessage(json);

        for (int i = 0; i < targetSessions.size(); i += MAX_BATCH_SIZE) {
            int endIndex = Math.min(i + MAX_BATCH_SIZE, targetSessions.size());
            List<WebSocketSession> batch = targetSessions.subList(i, endIndex);

            CompletableFuture.runAsync(() -> {
                sendToAllSessions(batch, textMessage, room);

                // Small delay between batches for very large groups
                if (endIndex < targetSessions.size()) {
                    try {
                        Thread.sleep(BATCH_DELAY_MS);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                }
            }, broadcastExecutor);
        }
    }

    // OPTION 3: With Rate Limiting (Recommended for production)
    private final Map<String, RateLimiter> userRateLimiters = new ConcurrentHashMap<>();

    public void groupMsgWithRateLimit(WebSocketSession session, Map<String, String> payload) {
        String room = getParam(session, "conversationId");
        String user = (String) session.getAttributes().get("userId");
        String msg = payload.get("msg");

        // Rate limiting check
        if (isRateLimited(user)) {
            log.warn("User {} is rate limited in room {}", user, room);
            sendErrorToSession(session, "Rate limit exceeded. Please slow down.");
            return;
        }

        // Input validation
        if (msg == null || msg.trim().isEmpty()) {
            sendErrorToSession(session, "Message cannot be empty");
            return;
        }

        if (msg.length() > 1000) { // Max message length
            sendErrorToSession(session, "Message too long");
            return;
        }

        chatService.saveMessage(room, user, msg, MessageStatus.DELIVERED);
        broadcastMessageOptimized(room, user, msg, session);
    }

    private boolean isRateLimited(String userId) {
        RateLimiter limiter = userRateLimiters.computeIfAbsent(userId,
                k -> RateLimiter.create(5.0)); // 5 messages per second max
        return !limiter.tryAcquire();
    }

    private void sendErrorToSession(WebSocketSession session, String errorMessage) {
        try {
            MessageResponseVO errorResponse = MessageResponseVO.builder()
                    .type("error")
                    .msg(errorMessage)
                    .build();
            String json = objectMapper.writeValueAsString(errorResponse);
            session.sendMessage(new TextMessage(json));
        } catch (Exception e) {
            log.error("Failed to send error message to session: {}", e.getMessage());
        }
    }


    // Scheduled cleanup of dead connections
    @Scheduled // Every 30 seconds
    public void cleanupDeadConnections() {
        roomSessions.entrySet().removeIf(entry -> {
            entry.getValue().removeIf(session -> !session.isOpen());
            return entry.getValue().isEmpty();
        });

        log.debug("Cleaned up dead connections. Active rooms: {}", roomSessions.size());
    }

    // Shutdown hook
    @PreDestroy
    public void shutdown() {
        log.info("Shutting down broadcast executor...");
        broadcastExecutor.shutdown();
        try {
            if (!broadcastExecutor.awaitTermination(10, SECONDS)) {
                log.warn("Executor did not terminate gracefully, forcing shutdown");
                broadcastExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            broadcastExecutor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }*/
}
