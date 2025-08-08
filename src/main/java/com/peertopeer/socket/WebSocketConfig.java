package com.peertopeer.socket;

import com.peertopeer.socket.handlers.GroupChatWebSocketHandler;
import com.peertopeer.socket.handlers.PresenceWebSocketHandler;
import com.peertopeer.socket.handlers.PrivateChatWebSocketHandler;
import com.peertopeer.auth.interceptor.JwtHandshakeInterceptor;
import com.peertopeer.service.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

import java.util.Scanner;

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
    private final JwtHandshakeInterceptor jwtHandshakeInterceptor;

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {

        registry.addHandler(new PrivateChatWebSocketHandler(privateChat, statusService,
                        presenceService), "/chat")
                .addInterceptors(jwtHandshakeInterceptor)
                .setAllowedOriginPatterns("*");

        registry.addHandler(new PresenceWebSocketHandler(presenceService, conversationService), "/presence")
                .addInterceptors(jwtHandshakeInterceptor)
                .setAllowedOriginPatterns("*");

        registry.addHandler(new GroupChatWebSocketHandler(statusService, presenceService,
                        groupChatService), "/group-chat")
                .addInterceptors(jwtHandshakeInterceptor)
                .setAllowedOriginPatterns("*");
    }
}
