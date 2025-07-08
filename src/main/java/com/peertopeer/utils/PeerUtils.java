package com.peertopeer.utils;

import com.peertopeer.enums.MessageStatus;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Objects;
import java.util.Optional;

public class PeerUtils {

    public static String getParam(WebSocketSession session, String key) {
        return Optional.ofNullable(
                UriComponentsBuilder
                        .fromUri(Objects.requireNonNull(session.getUri()))
                        .build()
                        .getQueryParams()
                        .getFirst(key)
        ).orElse("");
    }

    public static MessageStatus getMessageStatus(boolean online, boolean isOnline) {
        MessageStatus status;
        if (online && isOnline) {
            status = MessageStatus.SEEN;
        } else if (online) {
            status = MessageStatus.DELIVERED;
        } else {
            status = MessageStatus.SEND;
        }
        return status;
    }

}
