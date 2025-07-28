package com.peertopeer;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.util.Map;

@SpringBootApplication
public class PeerToPeerApplication {

    public static void main(String[] args) {
        SpringApplication.run(PeerToPeerApplication.class, args);
    }


    /*
    Optional: Custom handshake interceptor for debugging
    */





}
