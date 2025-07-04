package com.peertopeer.service.impl;

import com.peertopeer.service.PresenceService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PresenceServiceImpl implements PresenceService {

    private final RedisTemplate<String, String> redis;

    public static final String ONLINE_USERS_KEY = "online_users";


    @Override
    public void markOnline(String userId) {
        redis.opsForSet().add(ONLINE_USERS_KEY, userId);
    }

    @Override
    public void markOffline(String userId) {
        redis.opsForSet().remove(ONLINE_USERS_KEY, userId);
    }

    @Override
    public boolean isOnline(String userId) {
        return Boolean.TRUE.equals(redis.opsForSet().isMember(ONLINE_USERS_KEY, userId));
    }

    @Override
    public Set<String> getOnlineUsers() {
        return redis.opsForSet().members(ONLINE_USERS_KEY);
    }



    /// ///////////////////////////


    @Override
    public String key(String chatId, String userId) {
        return "typing:" + chatId + ":" + userId;
    }

    @Override
    public void setTyping(String chatId, String userId) {
        String key = key(chatId, userId);
        redis.opsForValue().set(key, "true", Duration.ofSeconds(10)); // auto-expire
    }

    @Override
    public void clearTyping(String chatId, String userId) {
        redis.delete(key(chatId, userId));
    }

    @Override
    public Set<String> getTypingUsers(String chatId) {
        // Need to scan keys: typing:<chatId>:*
        Set<String> keys = redis.keys("typing:" + chatId + ":*");
        if (keys == null) return Set.of();

        return keys.stream()
                .map(k -> k.substring(k.lastIndexOf(":") + 1)) // extract userId
                .collect(Collectors.toSet());
    }
}
