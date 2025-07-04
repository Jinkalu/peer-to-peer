package com.peertopeer.service;

import java.util.Set;

public interface PresenceService {
    void markOnline(String connectedUser);

    void markOffline(String userId);

    boolean isOnline(String target);

    Set<String> getOnlineUsers();


    String key(String chatId, String userId);

    void setTyping(String chatId, String userId);

    void clearTyping(String chatId, String userId);

    Set<String> getTypingUsers(String chatId);
}
