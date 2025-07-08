package com.peertopeer.config;

import com.peertopeer.config.handlers.ChatWebSocketHandler;
import com.peertopeer.config.handlers.PresenceWebSocketHandler;
import com.peertopeer.service.ChatService;
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

    private final ChatService chatService;
    private final PresenceService presenceService;


    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {

        registry.addHandler(new ChatWebSocketHandler(presenceService, chatService), "/chat")
                .setAllowedOrigins("*")
                .addInterceptors(new HttpSessionHandshakeInterceptor());

        registry.addHandler(new PresenceWebSocketHandler(presenceService), "/presence")
                .setAllowedOrigins("*")
                .addInterceptors(new HttpSessionHandshakeInterceptor());
    }

}
