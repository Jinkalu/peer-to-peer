package com.peertopeer.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.peertopeer.entity.Conversations;
import com.peertopeer.entity.Users;
import com.peertopeer.enums.ConversationType;
import com.peertopeer.enums.MessageStatus;
import com.peertopeer.repository.ConversationsRepository;
import com.peertopeer.repository.UserRepository;
import com.peertopeer.service.ChatService;
import com.peertopeer.service.GroupChatService;
import com.peertopeer.service.JwtService;
import com.peertopeer.service.PresenceService;
import com.peertopeer.utils.ChatUtils;
import com.peertopeer.vo.*;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

import static com.peertopeer.socket.handlers.GroupChatWebSocketHandler.roomSessions;
import static com.peertopeer.utils.PeerUtils.getParam;
import static com.peertopeer.utils.PeerUtils.isEmpty;

@Slf4j
@Service
@RequiredArgsConstructor
public class GroupChatServiceImpl implements GroupChatService {


    private final ChatService chatService;
    private final UserRepository userRepository;
    private final PresenceService presenceService;

    private final ObjectMapper mapper = new ObjectMapper();

    private final ConversationsRepository conversationsRepository;


    @Override
    public GroupVO createGroup(GroupCreationVO request) {
        Conversations conversation = conversationsRepository.saveAndFlush(Conversations.builder()
                .users(!CollectionUtils.isEmpty(request.getUsers()) ?
                        new HashSet<>(userRepository.findAllById(request.getUsers())) : null)
                .type(ConversationType.GROUP_CHAT)
                .createdBy(userRepository.findByUsername(JwtService.getCurrentUserUUID())
                        .orElseThrow().getId())
                .conversationName(request.getGroupName())
                .build());
        return GroupVO.builder()
                .groupDetails(ConversationVO.builder()
                        .id(conversation.getId())
                        .owner(userRepository.findById(request.getUserId())
                                .map(ChatUtils::mapToMembersVO)
                                .orElse(null))
                        .type(ConversationType.GROUP_CHAT)
                        .groupName(conversation.getConversationName())
                        .createdAt(conversation.getCreatedAt())
                        .build())
                .build();
    }

    @Override
    public void addMember(GroupCreationVO request) {
        Optional<Conversations> group = conversationsRepository.findById(request.getConversationId());
        group.ifPresent(conversations -> conversations.getUsers()
                .addAll(new HashSet<>(userRepository.findAllById(request.getUsers()))));
    }

    @Override
    @Transactional
    public void removeMember(Long conversationId, Long memberId) {
        Optional<Conversations> group = conversationsRepository.findById(conversationId);
        group.ifPresent(conversations -> {
            conversations.getUsers().removeIf(user -> Objects.equals(user.getId(), memberId));
            conversationsRepository.save(conversations);
        });
    }

    @Override
    public GroupVO membersList(Long conversationId, int page, int size) {
        GroupVO response = new GroupVO();
        conversationsRepository.findById(conversationId).ifPresent(conversation -> {
            Page<Users> users = conversationsRepository.groupMembers(conversationId, PageRequest.of(page, size));
            response.setGroupDetails(ConversationVO.builder()
                    .id(conversationId)
                    .type(conversation.getType())
                    .owner(userRepository.findById(conversation.getCreatedBy())
                            .map(ChatUtils::mapToMembersVO).orElse(null))
                    .groupName(conversation.getConversationName())
                    .members(users.getContent().stream()
                            .map(user -> {
                                UserVO userVO = ChatUtils.mapToMembersVO(user);
                                userVO.setIsOwner(Objects.equals(conversation.getCreatedBy(), userVO.getId()));
                                return userVO;
                            }).toList())
                    .build());
        });
        return response;
    }

    @Override
    public void groupMsg(WebSocketSession session, Map<String, String> payload) {
        String room = getParam(session, "conversationId");
        String user = (String) session.getAttributes().get("userId");
        String msg = payload.get("msg");

        chatService.saveMessage(room, user, msg, MessageStatus.DELIVERED);
        roomSessions.getOrDefault(room, Collections.emptySet()).stream()
                .filter(peerSession -> peerSession.isOpen()
                        && !peerSession.getAttributes().get("userId").equals(user))
                .forEach(peerSession -> CompletableFuture.runAsync(() -> {
                    try {
                        MessageResponseVO message = MessageResponseVO.builder()
                                .type("msg")
                                .sender(user)
                                .senderUsername((String) session.getAttributes().get("username"))
                                .msg(msg)
                                .build();
                        String json = mapper.writeValueAsString(message);
                        peerSession.sendMessage(new TextMessage(json));
                    } catch (IOException e) {
                        log.info("Exception occurred while sending to group {}", room);
                    }
                }));
    }

    @Override
    public String getGroupId(WebSocketSession session) {
        String conversationId;
        conversationId = getParam(session, "conversationId");
        if (!isEmpty(conversationId)) {
            roomSessions.computeIfAbsent(conversationId,
                    r -> ConcurrentHashMap.newKeySet()).add(session);
            session.getAttributes().put("room", conversationId);
        }
        return conversationId;
    }

    @Override
    public void removeUserFromRoom(WebSocketSession session) {
        String user = (String) session.getAttributes().get("userId");
        String conversationId = session.getAttributes().get("room").toString();
        presenceService.offScreen(user, conversationId);

        String room = (String) session.getAttributes().get("room");
        if (room != null) {
            Set<WebSocketSession> sessions = roomSessions.get(room);
            if (sessions != null) {
                sessions.remove(session);
                if (sessions.isEmpty()) roomSessions.remove(room);
            }
        }
    }

}
