package com.peertopeer.config.handlers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.peertopeer.entity.Conversations;
import com.peertopeer.entity.Message;
import com.peertopeer.entity.UserConversation;
import com.peertopeer.enums.ConversationStatus;
import com.peertopeer.enums.ConversationType;
import com.peertopeer.enums.MessageStatus;
import com.peertopeer.enums.SourceType;
import com.peertopeer.repository.ConversationsRepository;
import com.peertopeer.repository.MessageRepository;
import com.peertopeer.repository.UserConversationRepository;
import com.peertopeer.service.PresenceService;
import com.peertopeer.vo.ChatUserVO;
import com.peertopeer.vo.ConversationVO;
import com.peertopeer.vo.MessageVO;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import static com.peertopeer.utils.PeerUtils.getParam;

@Component
@RequiredArgsConstructor
public class ChatWebSocketHandler extends TextWebSocketHandler {

    private final Map<String, Set<WebSocketSession>> roomSessions = new ConcurrentHashMap<>();
    private final Map<String, WebSocketSession> userSessions = new ConcurrentHashMap<>();
    private final Map<String, Set<String>> chatScreenPresence = new ConcurrentHashMap<>();


    private final ConversationsRepository conversationsRepository;
    private final MessageRepository messageRepository;
    private final UserConversationRepository userConversationRepository;

    private final PresenceService presenceService;

    @Async
    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        String user = getParam(session, "user");
        userSessions.put(user, session);

        String type = getParam(session, "type");
        if ("group".equals(type)) {
            String room = getParam(session, "target");
            roomSessions.computeIfAbsent(room, r -> ConcurrentHashMap.newKeySet()).add(session);
            session.getAttributes().put("room", room);
        }

        session.getAttributes().put("user", user);
        session.getAttributes().put("type", type);
    }

    @Async
    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        Map<String, String> payload = new ObjectMapper().readValue(message.getPayload(), Map.class);
        String type = (String) session.getAttributes().get("type");

/*        if ("joinChatScreen".equals(payload.get("type"))) {
            joinChatScreen(session, payload.get("chatId"), (String) session.getAttributes().get("user"));
            return;
        } else if ("leaveChatScreen".equals(payload.get("type"))) {
            leaveChatScreen(payload.get("chatId"), (String) session.getAttributes().get("user"));
            return;
        }*/

        if ("typing".equals(payload.get("type"))) {
            handleTypingStatus(session, payload);
            return;
        }

        if ("group".equals(type)) {
            groupMsg(session, payload);
        } else if ("private".equals(type)) {
            privateMsg(session, payload);
        }
    }

    private void joinChatScreen(WebSocketSession session, String chatId, String userId) {
        chatScreenPresence.computeIfAbsent(chatId, k -> ConcurrentHashMap.newKeySet()).add(userId);
    }

    private void leaveChatScreen(String chatId, String userId) {
        chatScreenPresence.getOrDefault(chatId, Set.of()).remove(userId);
    }


    @Async
    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        String user = (String) session.getAttributes().get("user");
        userSessions.remove(user);

        String room = (String) session.getAttributes().get("room");
        if (room != null) {
            Set<WebSocketSession> sessions = roomSessions.get(room);
            if (sessions != null) {
                sessions.remove(session);
                if (sessions.isEmpty()) roomSessions.remove(room);
            }
        }
    }

    private void handleTypingStatus(WebSocketSession session, Map<String, String> payload) throws IOException {
        String fromUser = (String) session.getAttributes().get("user");
        String toUser = payload.get("to");


        String chatId = getPrivateChatId(fromUser, toUser);

        if (Objects.equals(payload.get("isTyping"), "true")) {
            presenceService.setTyping(chatId, fromUser);
        } else {
            presenceService.clearTyping(chatId, fromUser);
        }


        WebSocketSession peerSession = userSessions.get(toUser);
        if (peerSession != null && peerSession.isOpen()) {
            Map<String, Object> typingMessage = Map.of(
                    "type", "typing",
                    "from", fromUser,
                    "isTyping", payload.get("isTyping")
            );
            String json = new ObjectMapper().writeValueAsString(typingMessage);
            peerSession.sendMessage(new TextMessage(json));
        }
    }

    private String getPrivateChatId(String user1, String user2) {
        return user1.compareTo(user2) < 0
                ? user1 + "_" + user2
                : user2 + "_" + user1;
    }





    private void groupMsg(WebSocketSession session, Map<String, String> payload) throws IOException {
        String room = (String) session.getAttributes().get("room");
        String user = getParam(session, "user");
        String msg = payload.get("msg");
        for (WebSocketSession s : roomSessions.getOrDefault(room, Set.of())) {
            if (s.isOpen()) {
                s.sendMessage(new TextMessage(String.format("{\"from\":\"%s\", \"msg\":%s}",
                        user, msg)));
            }
        }
    }





    private void privateMsg(WebSocketSession session, Map<String, String> payload) throws IOException {
        String toUser = payload.get("to");
        String fromUser = (String) session.getAttributes().get("user");
//        MessageStatus status = MessageStatus.valueOf(payload.get("status"));
        WebSocketSession peerSession = userSessions.get(toUser);
        boolean isOnline = peerSession != null && peerSession.isOpen();

        String msg = payload.get("msg");
        if (isOnline) {
            Map<String, String> response = Map.of(
                    "from", fromUser,
                    "msg", msg
            );
            peerSession.sendMessage(new TextMessage(new ObjectMapper().writeValueAsString(response)));

            // ✅ Send delivered status to sender
            Map<String, Object> deliveredStatus = Map.of(
                    "type", "status",
                    "status", MessageStatus.SEND,
                    "to", toUser,
                    "msg", msg
                    // Optionally include messageId if available
            );
            WebSocketSession senderSession = userSessions.get(fromUser);
            if (senderSession != null && senderSession.isOpen()) {
                senderSession.sendMessage(new TextMessage(new ObjectMapper().writeValueAsString(deliveredStatus)));
            }
        }
        createConversation(fromUser, toUser, msg, null, null, ConversationType.PRIVATE,
                SourceType.PRIVATE, false, MessageStatus.SEND);
    }


    @Async("taskExecutor")
    public void createConversation(String senderUUID, String receiverUUID, String message,
                                   List<MultipartFile> medias, Integer sourceId, ConversationType conversationType,
                                   SourceType sourceType, Boolean isStory, MessageStatus status) {
        Long conversationId = conversationsRepository.findConversationIdReceiverUUIDAndSenderUUID(receiverUUID, senderUUID);
        Conversations conversation;
        if (Objects.isNull(conversationId) || conversationId == 0L) {
            conversation = conversationsRepository.save(Conversations.builder()
                    .type(conversationType)
                    .isPinned(false)
                    .status(ConversationStatus.ACTIVE)
                    .readStatus(false)
                    .build());
            saveMessages(senderUUID, receiverUUID, message, medias, conversation, sourceId, sourceType, status);
            saveToUserConversation(senderUUID, receiverUUID, conversation);
        } else {
            conversation = getConversation(conversationId);
            conversation.setUpdatedAt(Instant.now().toEpochMilli());
            saveMessages(senderUUID, receiverUUID, message, medias, conversation, sourceId, sourceType, status);
        }

    }

    private Conversations getConversation(Long conversationId) {
        return conversationsRepository.findById(conversationId)
                .orElseThrow(/*() -> new ValidationException(ApiError.builder()
                        .errors(List.of("Invalid conversation id"))
                        .status(HttpStatus.BAD_REQUEST.name())
                        .code(String.valueOf(HttpStatus.BAD_REQUEST.value()))
                        .httpStatus(HttpStatus.BAD_REQUEST)
                        .build())*/);

    }

    private ConversationVO getConversationVO(Conversations conversation, ChatUserVO userVo, Boolean isMessage) {
        return ConversationVO.builder()
                .id(conversation.getId())
                .conversationName(userVo.getUserName())
                .type(conversation.getType())
                .status(conversation.getStatus())
                .readStatus(conversation.getReadStatus())
                .isPined(conversation.getIsPinned())
                .messages(isMessage ? mapMessageVO(conversation.getMessages()) : new ArrayList<>())
                .receiverDetails(userVo)
                .createdAt(conversation.getCreatedAt())
                .updatedAt(conversation.getUpdatedAt())
                .build();
    }


    private void saveMessages(String senderUUID, String receiverUUID, String message,
                              List<MultipartFile> medias, Conversations conversation,
                              Integer sourceId, SourceType sourceType, MessageStatus status) {
        if (!Objects.isNull(medias) && !medias.get(0).isEmpty()) {
            medias.forEach(media -> messageRepository.save(Message.builder()
                    .senderUUID(senderUUID)
                    .receiverUUID(receiverUUID)
                    .conversation(conversation)
                    .status(status)
                    .createdAt(Instant.now().toEpochMilli())
//                    .updateAt(Instant.now().toEpochMilli())
                    .message(Objects.requireNonNullElse(message, ""))
//                  .mediaURL(saveChatMedia(media))
//                    .sourceId(sourceId)
                    .sourceType(sourceType)
                    .build()));
        } else {
            messageRepository.save(Message.builder()
                    .senderUUID(senderUUID)
                    .receiverUUID(receiverUUID)
                    .conversation(conversation)
                    .status(MessageStatus.SEND)
                    .createdAt(Instant.now().toEpochMilli())
//                    .updateAt(Instant.now().toEpochMilli())
//                    .sourceId(sourceId)
                    .sourceType(sourceType)
                    .message(Objects.requireNonNullElse(message, ""))
                    .build());
        }
    }

    private void saveToUserConversation(String senderUUID, String receiverUUID, Conversations conversation) {
        userConversationRepository.saveAll(List.of(UserConversation.builder()
                .conversationId(conversation.getId())
                .userId(senderUUID)
                .build(), UserConversation.builder()
                .conversationId(conversation.getId())
                .userId(receiverUUID)
                .build()));
    }


    private List<MessageVO> mapMessageVO(List<Message> messages) {
        return messages.stream()
                .map(message -> {
                    return MessageVO.builder()
                            .id(message.getId())
                            .senderUUID(message.getSenderUUID())
                            .receiverUUID(message.getReceiverUUID())
                            .status(message.getStatus())
                            .message(Objects.requireNonNullElse(message.getMessage(), ""))
                            .mediaURL(Objects.requireNonNullElse(message.getMediaURL(), ""))
                            .reaction(Objects.requireNonNullElse(message.getReaction(), ""))
                            .sourceType(message.getSourceType().name())
//                            .sourceId(message.getSourceType().equals(SourceType.PRIVATE) ? message.getConversation().getId().intValue()
//                                    : message.getSourceId())
                            .createdAt(message.getCreatedAt())
//                            .updateAt(message.getUpdateAt())
                            .build();
                }).collect(Collectors.toList());
    }

    public WebSocketSession getUserSession(String sender) {
        return userSessions.get(sender);
    }
}

