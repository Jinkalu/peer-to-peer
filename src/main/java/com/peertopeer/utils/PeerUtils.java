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

  /*  public <T> T parseAndValidate(String request, Class<T> clazz) {
        try {
            if (isEmpty(request)) {
//                throw buildValidationException(List.of("Request body is empty"));
                throw new RuntimeException("Body is empty");
            }

            T dto = new ObjectMapper().readValue(request, clazz);
            Set<ConstraintViolation<T>> violations = validator.validate(dto);

            if (!violations.isEmpty()) {
                List<String> errors = new ArrayList<>();
                for (ConstraintViolation<T> violation : violations) {
                    errors.add(violation.getMessage());
                }
                throw new RuntimeException("Body is empty");
            }
            return dto;

        } catch (ValidationException ve) {
            throw ve;
        } catch (Exception ex) {
            throw new RuntimeException("Body is empty");
        }
    }*/

  /*  private ValidationException buildValidationException(List<String> errors) {
        return new ValidationException(ApiError.builder()
                .status(HttpStatus.BAD_REQUEST.name())
                .code(String.valueOf(HttpStatus.BAD_REQUEST.value()))
                .httpStatus(HttpStatus.BAD_REQUEST)
                .errors(errors)
                .build());
    }*/
}
