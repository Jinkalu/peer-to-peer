package com.peertopeer.utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.peertopeer.enums.MessageStatus;

import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.*;

public class PeerUtils {

    public static String getParam(WebSocketSession session, String key) {
        return UriComponentsBuilder
                .fromUri(Objects.requireNonNull(session.getUri()))
                .build()
                .getQueryParams()
                .getFirst(key);
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

    public static boolean isEmpty(String user) {
        return user == null || user.isBlank();
    }

    public static String getPrivateChatId(String user1, String user2) {
        return user1.compareTo(user2) < 0
                ? user1 + "_" + user2
                : user2 + "_" + user1;
    }

    public static boolean isValidEmoji(String input) {
//        if (input == null || input.isEmpty()) return false;

        int codePointCount = input.codePointCount(0, input.length());
        if (codePointCount != 1) return false; // Ensure it's a single emoji

        int codePoint = input.codePointAt(0);

        // Emoji ranges (simplified; customize as needed)
        return (codePoint >= 0x1F600 && codePoint <= 0x1F64F) || // Emoticons
                (codePoint >= 0x1F300 && codePoint <= 0x1F5FF) || // Misc Symbols and Pictographs
                (codePoint >= 0x1F680 && codePoint <= 0x1F6FF) || // Transport and Map
                (codePoint >= 0x2600 && codePoint <= 0x26FF)   || // Misc symbols
                (codePoint >= 0x2700 && codePoint <= 0x27BF);     // Dingbats
    }

}
