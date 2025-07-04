package com.peertopeer.utils;

import org.springframework.util.MultiValueMap;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Objects;

public class PeerUtils {

    public static String getParam(WebSocketSession session, String key) {
        MultiValueMap<String, String> queryParams = UriComponentsBuilder.fromUri(Objects.requireNonNull(session.getUri()))
                .build()
                .getQueryParams();
        System.out.println(
                queryParams
        );
        return queryParams
                .getFirst(key);
    }

}
