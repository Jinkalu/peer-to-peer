package com.peertopeer.service.impl;

import com.peertopeer.entity.Users;
import com.peertopeer.utils.ChatUtils;
import lombok.RequiredArgsConstructor;
import com.peertopeer.vo.ConversationVO;
import com.peertopeer.service.JwtService;
import com.peertopeer.entity.Conversations;
import com.peertopeer.enums.ConversationType;
import org.springframework.stereotype.Service;
import com.peertopeer.repository.UserRepository;
import com.peertopeer.service.ConversationService;
import org.springframework.cache.annotation.Cacheable;
import com.peertopeer.repository.ConversationsRepository;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class ConversationServiceImpl implements ConversationService {

    private final UserRepository userRepository;
    private final ConversationsRepository conversationsRepository;

    @Override
    public List<ConversationVO> listConversations() {
        Users user = JwtService.getUserDetails();
        Long userId = user.getId();
        List<Conversations> conversations = conversationsRepository.findByUsers_Id(userId);
        return conversations.stream()
                .map(conversation -> {
                    ConversationVO conversationVO = ChatUtils.mapToConversationVO(userId, conversation);
                    boolean isGroup = Objects.equals(conversation.getType(), ConversationType.GROUP_CHAT);
                    conversationVO.setUnreadCount(unreadCountInConvo(userId, conversation.getId()));
                    conversationVO.setOwner(isGroup ? ChatUtils.mapToMembersVO(userRepository.findById(conversation.getCreatedBy()).get()) : null);
                    conversationVO.setPeerUser(!isGroup ? ChatUtils.mapToMembersVO(conversationsRepository.getPeerUser(userId, conversation.getId())) : null);
                    return conversationVO;
                }).toList();
    }

    @Override
    public Long unreadCountInConvo(Long currentUserUid, Long conversationId) {
        return conversationsRepository.unreadCount(currentUserUid.toString(), conversationId);
    }

    @Override
    @Cacheable(value = "peerUserCache", key = "#conversationId + '-' + #userId")
    public String findPeerUser(Long conversationId, Long userId) {
        return conversationsRepository.findPeerUser(conversationId, userId).toString();
    }

    @Override
    public Long createConversation(Long peerUserId) {
        Users currentUser = JwtService.getUserDetails();
        List<Users> userList = userRepository.findAllById(List.of(currentUser.getId(), peerUserId));
        return conversationsRepository.saveAndFlush(Conversations.builder()
                .users(new HashSet<>(userList))
                .type(ConversationType.PRIVATE_CHAT)
                .build()).getId();
    }

}
