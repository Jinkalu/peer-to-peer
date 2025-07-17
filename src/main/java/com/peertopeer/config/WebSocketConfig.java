package com.peertopeer.config;

import com.peertopeer.config.handlers.ChatWebSocketHandler;
import com.peertopeer.config.handlers.PresenceWebSocketHandler;
import com.peertopeer.service.ChatService;
import com.peertopeer.service.PresenceService;
import com.peertopeer.service.PrivateChat;
import com.peertopeer.service.StatusService;
import jdk.jshell.Snippet;
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

    private final PrivateChat privateChat;
    private final PresenceService presenceService;
    private final StatusService statusService;
    private final ChatService chatService;


    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {

        registry.addHandler(new ChatWebSocketHandler(presenceService, statusService, privateChat), "/chat")
                .setAllowedOrigins("*")
                .addInterceptors(new HttpSessionHandshakeInterceptor());

        registry.addHandler(new PresenceWebSocketHandler(presenceService,chatService), "/presence")
                .setAllowedOrigins("*")
                .addInterceptors(new HttpSessionHandshakeInterceptor());
    }

}
