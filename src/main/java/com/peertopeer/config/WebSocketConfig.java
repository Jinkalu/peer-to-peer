package com.peertopeer.config;

import com.peertopeer.config.handlers.ChatWebSocketHandler;
import com.peertopeer.config.handlers.OnScreenPresenceWebSocketHandler;
import com.peertopeer.config.handlers.PresenceWebSocketHandler;
import com.peertopeer.repository.*;
import com.peertopeer.service.PresenceService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;
import org.springframework.web.socket.server.support.HttpSessionHandshakeInterceptor;

@Configuration
@EnableWebSocket
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketConfigurer {

    private final UserConversationRepository  userConversationRepository;
    private final ConversationsRepository conversationsRepository;
    private final MessageRepository messageRepository;
    private final PresenceService presenceService;


    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {

        registry.addHandler(new ChatWebSocketHandler(conversationsRepository,
                messageRepository,userConversationRepository,presenceService), "/chat")
                .setAllowedOrigins("*")
                .addInterceptors(new HttpSessionHandshakeInterceptor());

        registry.addHandler(new PresenceWebSocketHandler(presenceService), "/presence")
                .setAllowedOrigins("*")
                .addInterceptors(new HttpSessionHandshakeInterceptor());

        registry.addHandler(new OnScreenPresenceWebSocketHandler(presenceService), "/on-screen-presence")
                .setAllowedOrigins("*")
                .addInterceptors(new HttpSessionHandshakeInterceptor());

    }

}
