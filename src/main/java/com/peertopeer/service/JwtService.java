package com.peertopeer.service;

import com.peertopeer.entity.Users;
import org.springframework.security.core.context.SecurityContextHolder;

public interface JwtService {
    String generateToken(Users username);

    String extractUsername(String token);

    String extractId(String token);

    boolean isTokenValid(String token, String username);

    boolean isTokenExpired(String token);

    static String getCurrentUserUUID() {
        return SecurityContextHolder.getContext().getAuthentication().getName();
    }
}
