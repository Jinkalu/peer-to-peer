package com.peertopeer.auth.interceptor;


import com.peertopeer.service.JwtService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtHandshakeInterceptor implements HandshakeInterceptor {

    private final JwtService jwtUtil;

    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                   WebSocketHandler wsHandler, Map<String, Object> attributes) {
        try {
            String token = extractToken(request);

            if (token == null) {
                log.warn("No JWT token provided in WebSocket handshake");
                response.setStatusCode(HttpStatus.UNAUTHORIZED);
                return false;
            }

            if (jwtUtil.isTokenExpired(token)) {
                log.warn("Invalid JWT token provided in WebSocket handshake");
                response.setStatusCode(HttpStatus.UNAUTHORIZED);
                return false;
            }

            String username = jwtUtil.extractUsername(token);
            String userId = jwtUtil.extractId(token);

            attributes.put("username", username);
            attributes.put("userId", userId);
            attributes.put("token", token);

            log.info("WebSocket handshake successful for user: {}", username);
            return true;

        } catch (Exception e) {
            log.error("Error during WebSocket handshake authentication", e);
            response.setStatusCode(HttpStatus.INTERNAL_SERVER_ERROR);
            return false;
        }
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
                               WebSocketHandler wsHandler, Exception exception) {
        if (exception != null) {
            log.error("WebSocket handshake failed", exception);
        }
    }

    private String extractToken(ServerHttpRequest request) {
        String authHeader = request.getHeaders().getFirst("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }

        URI uri = request.getURI();
        String query = uri.getQuery();
        if (query != null) {
            try {
                String[] params = query.split("&");
                for (String param : params) {
                    String[] keyValue = param.split("=", 2);
                    if (keyValue.length == 2 && "token".equals(keyValue[0])) {
                        return java.net.URLDecoder.decode(keyValue[1], StandardCharsets.UTF_8);
                    }
                }
            } catch (Exception e) {
                log.warn("Error parsing query parameters for token extraction", e);
            }
        }

        String cookieHeader = request.getHeaders().getFirst("Cookie");
        if (cookieHeader != null) {
            String[] cookies = cookieHeader.split(";");
            for (String cookie : cookies) {
                String[] parts = cookie.trim().split("=", 2);
                if (parts.length == 2 && "jwt".equals(parts[0])) {
                    return parts[1];
                }
            }
        }

        return null;
    }
}