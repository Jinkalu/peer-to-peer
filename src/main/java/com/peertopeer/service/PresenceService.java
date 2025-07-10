package com.peertopeer.service;

import java.util.Set;

public interface PresenceService {


    void markOnline(String connectedUser);

    void markOffline(String userId);

    boolean isOnline(String target);

    Set<String> getOnlineUsers();


    Set<String> getOnScreenUsers(String conversationId);

    String key(String chatId, String userId);

    void setTyping(String chatId, String userId);

    void clearTyping(String chatId, String userId);

    Set<String> getTypingUsers(String chatId);

    void setOnScreen(String conversationId, String receiver);


    void offScreen(String userId, String conversationId);

    boolean isOnScreen(String userId, String conversationId);
}
