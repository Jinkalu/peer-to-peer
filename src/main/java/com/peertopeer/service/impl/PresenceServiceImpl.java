package com.peertopeer.service.impl;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.RemovalCause;
import com.peertopeer.service.ChatService;
import com.peertopeer.service.PresenceService;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class PresenceServiceImpl implements PresenceService {

    private final ChatService chatService;

    // Caffeine caches for all presence data
    private Cache<String, LocalDateTime> onlineUsersCache;
    private Cache<String, LocalDateTime> typingUsersCache;
    private Cache<String, Set<String>> conversationUsersCache;
    private Cache<String, LocalDateTime> lastActivityCache;

    // In-memory sets for immediate access
    private final Set<String> currentOnlineUsers = ConcurrentHashMap.newKeySet();
    private final Map<String, Set<String>> conversationScreenUsers = new ConcurrentHashMap<>();

    private volatile boolean shutdownExecuted = false;

    @PostConstruct
    public void initializeCaches() {
        // Cache for online users with last seen timestamp
        onlineUsersCache = Caffeine.newBuilder()
                .maximumSize(10000)
                .expireAfterWrite(5, TimeUnit.MINUTES) // User considered offline after 5 minutes
                .removalListener((String userId, LocalDateTime timestamp,RemovalCause cause) -> {
                    if (cause.wasEvicted()) {
                        currentOnlineUsers.remove(userId);
                        log.info("User {} marked offline due to inactivity", userId);
                    }
                })
                .build();

        // Cache for typing indicators - short expiry
        typingUsersCache = Caffeine.newBuilder()
                .maximumSize(50000)
                .expireAfterWrite(10, TimeUnit.SECONDS) // Typing expires after 10 seconds
                .removalListener((String key, LocalDateTime timestamp,RemovalCause cause) -> {
                    if (cause.wasEvicted()) {
                        log.info("Typing indicator expired for: {}", key);
                    }
                })
                .build();

        // Cache for conversation screen presence
        conversationUsersCache = Caffeine.newBuilder()
                .maximumSize(10000)
                .expireAfterWrite(30, TimeUnit.MINUTES)
                .removalListener((String key, Set<String> users,RemovalCause cause) -> {
                    if (cause.wasEvicted()) {
                        conversationScreenUsers.remove(key);
                    }
                })
                .build();

        // Cache for last activity tracking
        lastActivityCache = Caffeine.newBuilder()
                .maximumSize(20000)
                .expireAfterWrite(1, TimeUnit.HOURS)
                .build();

        log.info("Caffeine caches initialized successfully");
    }

    @PreDestroy
    public void preDestroy() {
        executeShutdownCleanup("PreDestroy");
    }

    @EventListener
    public void handleShutdown(ContextClosedEvent event) {
        executeShutdownCleanup("ContextClosedEvent");
    }

    private synchronized void executeShutdownCleanup(String trigger) {
        if (shutdownExecuted) {
            log.info("Shutdown cleanup already executed, skipping {}", trigger);
            return;
        }

        log.info("=== Executing shutdown cleanup via {} ===", trigger);
        try {
            // Clear all Caffeine caches
            if (onlineUsersCache != null) {
                onlineUsersCache.invalidateAll();
                log.info("Cleared online users cache");
            }

            if (typingUsersCache != null) {
                typingUsersCache.invalidateAll();
                log.info("Cleared typing users cache");
            }

            if (conversationUsersCache != null) {
                conversationUsersCache.invalidateAll();
                log.info("Cleared conversation users cache");
            }

            if (lastActivityCache != null) {
                lastActivityCache.invalidateAll();
                log.info("Cleared last activity cache");
            }

            // Clear in-memory collections
            currentOnlineUsers.clear();
            conversationScreenUsers.clear();
            log.info("Cleared in-memory collections");

            shutdownExecuted = true;
            log.info("Caffeine cache cleanup completed successfully via {}", trigger);

        } catch (Exception e) {
            log.error("Failed to clear Caffeine caches via {}: {}", trigger, e.getMessage());
            e.printStackTrace();
        }
    }

    @Override
    public void markOnline(String userId) {
        LocalDateTime now = LocalDateTime.now();

        // Update cache and in-memory set
        onlineUsersCache.put(userId, now);
        currentOnlineUsers.add(userId);
        lastActivityCache.put(userId, now);

        log.info("User {} marked online at {}", userId, now);

        // Update message status
        chatService.updateMessageStatus(userId);
    }

    @Override
    public boolean isOnline(String userId) {
        // Check in-memory set first (fastest)
        if (currentOnlineUsers.contains(userId)) {
            return true;
        }

        // Check cache
        LocalDateTime lastSeen = onlineUsersCache.getIfPresent(userId);
        boolean isOnline = lastSeen != null;

        // Update in-memory set if user is online
        if (isOnline) {
            currentOnlineUsers.add(userId);
        }
        return isOnline;
    }

    @Override
    public void markOffline(String userId) {
        // Remove from cache and in-memory set
        onlineUsersCache.invalidate(userId);
        currentOnlineUsers.remove(userId);

        // Update last activity
        lastActivityCache.put(userId, LocalDateTime.now());

        log.info("User {} marked offline", userId);
    }

    @Override
    public Set<String> getOnlineUsers() {
        // Return current online users
        return Set.copyOf(currentOnlineUsers);
    }

    @Override
    public Set<String> getOnScreenUsers(String conversationId) {
       return conversationUsersCache.getIfPresent(conversationId);
    }

    @Override
    public String key(String chatId, String userId) {
        return "typing:" + chatId + ":" + userId;
    }

    @Override
    public void setTyping(String chatId, String userId) {
        String key = key(chatId, userId);
        LocalDateTime now = LocalDateTime.now();

        // Store typing indicator with timestamp
        typingUsersCache.put(key, now);

        log.info("User {} started typing in chat {}", userId, chatId);
    }

    @Override
    public void clearTyping(String chatId, String userId) {
        String key = key(chatId, userId);
        typingUsersCache.invalidate(key);

        log.info("User {} stopped typing in chat {}", userId, chatId);
    }

    @Override
    public Set<String> getTypingUsers(String chatId) {
        String prefix = "typing:" + chatId + ":";

        // Filter cache entries by chat ID and extract user IDs
        return typingUsersCache.asMap().entrySet().stream()
                .filter(entry -> entry.getKey().startsWith(prefix))
                .map(entry -> entry.getKey().substring(prefix.length()))
                .collect(Collectors.toSet());
    }

    @Override
    public void setOnScreen(String conversationId, String userId) {
        // Get or create user set for this conversation
        Set<String> users = conversationScreenUsers.computeIfAbsent(
                conversationId,
                k -> ConcurrentHashMap.newKeySet()
        );
        users.add(userId);
        // Update cache
        conversationUsersCache.put(conversationId, Set.copyOf(users));
        log.info("User {} is now on screen for conversation {}", userId, conversationId);
    }

    @Override
    public void offScreen(String userId, String conversationId) {
        Set<String> users = conversationScreenUsers.get(conversationId);
        if (users != null) {
            users.remove(userId);

            // Update cache
            if (users.isEmpty()) {
                conversationScreenUsers.remove(conversationId);
                conversationUsersCache.invalidate(conversationId);
            } else {
                conversationUsersCache.put(conversationId, Set.copyOf(users));
            }
        }

        log.info("User {} is no longer on screen for conversation {}", userId, conversationId);
    }

    @Override
    public boolean isOnScreen(String userId, String conversationId) {
        Set<String> users = conversationScreenUsers.get(conversationId);
        return users != null && users.contains(userId);
    }

    // Additional utility methods

    /**
     * Get last activity time for a user
     */
    public LocalDateTime getLastActivity(String userId) {
        return lastActivityCache.getIfPresent(userId);
    }

    /**
     * Get users on screen for a conversation
     */
    public Set<String> getUsersOnScreen(String conversationId) {
        Set<String> users = conversationScreenUsers.get(conversationId);
        return users != null ? Set.copyOf(users) : Set.of();
    }

    /**
     * Manually refresh user's online status (heartbeat)
     */
    public void refreshUserActivity(String userId) {
        if (isOnline(userId)) {
            markOnline(userId); // Refresh timestamp
        }
    }

    /**
     * Get count of online users
     */
    public long getOnlineUserCount() {
        return currentOnlineUsers.size();
    }

    /**
     * Get all users currently typing in any chat
     */
    public Set<String> getAllTypingUsers() {
        return typingUsersCache.asMap().keySet().stream()
                .map(key -> key.substring(key.lastIndexOf(":") + 1))
                .collect(Collectors.toSet());
    }

    /**
     * Cleanup expired entries manually (useful for monitoring)
     */
    public void cleanupExpiredEntries() {
        onlineUsersCache.cleanUp();
        typingUsersCache.cleanUp();
        conversationUsersCache.cleanUp();
        lastActivityCache.cleanUp();
        log.info("Manual cleanup of expired cache entries completed");
    }

    /**
     * Get cache statistics for monitoring
     */
    public void printCacheStats() {
        log.info("=== Caffeine Cache Statistics ===");
        log.info("Online Users Cache: {}", onlineUsersCache.stats());
        log.info("Typing Users Cache: {}", typingUsersCache.stats());
        log.info("Conversation Users Cache: {}", conversationUsersCache.stats());
        log.info("Last Activity Cache: {}", lastActivityCache.stats());
        log.info("Current Online Users: {}", currentOnlineUsers.size());
        log.info("Conversations with Users: {}", conversationScreenUsers.size());
    }

    /**
     * Force remove user from all presence tracking
     */
    public void forceRemoveUser(String userId) {
        markOffline(userId);

        // Remove from all typing indicators
        typingUsersCache.asMap().entrySet().removeIf(entry ->
                entry.getKey().endsWith(":" + userId)
        );

        // Remove from all conversation screens
        conversationScreenUsers.values().forEach(users -> users.remove(userId));

        log.info("Force removed user {} from all presence tracking", userId);
    }
}