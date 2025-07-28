package com.peertopeer.config;

import com.peertopeer.config.handlers.GroupChatWebSocketHandler;
import com.peertopeer.config.handlers.PresenceWebSocketHandler;
import com.peertopeer.config.handlers.PrivateChatWebSocketHandler;
import com.peertopeer.service.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.util.Map;

@Slf4j
@Configuration
@EnableWebSocket
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketConfigurer {

    private final PrivateChat privateChat;
    private final StatusService statusService;
    private final PresenceService presenceService;
    private final GroupChatService groupChatService;
    private final ConversationService conversationService;

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {


        registry.addHandler(new PrivateChatWebSocketHandler(privateChat, statusService,
                        presenceService), "/chat")
                .setAllowedOriginPatterns("*")
                .addInterceptors(new org.springframework.web.socket.server.support.HttpSessionHandshakeInterceptor());

        registry.addHandler(new PresenceWebSocketHandler(presenceService, conversationService), "/presence")
                .setAllowedOriginPatterns("*")
                .addInterceptors(new org.springframework.web.socket.server.support.HttpSessionHandshakeInterceptor());

        registry.addHandler(new GroupChatWebSocketHandler(statusService, presenceService,
                        groupChatService), "/group-chat")
                .setAllowedOriginPatterns("*")
                .addInterceptors(new org.springframework.web.socket.server.support.HttpSessionHandshakeInterceptor());
    }


    @Bean
    public HandshakeInterceptor customHandshakeInterceptor() {
        return new HandshakeInterceptor() {

            @Override
            public boolean beforeHandshake(ServerHttpRequest request,
                                           org.springframework.http.server.ServerHttpResponse response,
                                           WebSocketHandler wsHandler, Map<String, Object> attributes) throws Exception {
                log.info("WebSocket handshake attempt from: {}", request.getRemoteAddress());
                log.info("Headers: {}", request.getHeaders());
                return true;
            }

            @Override
            public void afterHandshake(ServerHttpRequest request, org.springframework.http.server.ServerHttpResponse response,
                                       WebSocketHandler wsHandler, Exception exception) {
                if (exception == null) {
                    log.info("WebSocket handshake successful");
                } else {
                    log.info("WebSocket handshake failed: {}", exception.getMessage());
                }
            }
        };
    }
}
