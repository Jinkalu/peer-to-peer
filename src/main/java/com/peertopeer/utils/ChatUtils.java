package com.peertopeer.utils;

import com.peertopeer.entity.Conversations;
import com.peertopeer.entity.GroupMessageReaction;
import com.peertopeer.entity.Message;
import com.peertopeer.enums.ConversationType;
import com.peertopeer.vo.ConversationVO;
import com.peertopeer.vo.MessageVO;
import com.peertopeer.vo.UserVO;
import com.peertopeer.entity.Users;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;


@Slf4j
public class ChatUtils {

    public static UserVO mapToMembersVO(Users member) {
        return UserVO.builder()
                .id(member.getId())
                .username(member.getUsername())
                .build();
    }

    public static MessageVO mapToMessageVO(Message message) {
        log.info("message ID : : {}", message.getId());
        return MessageVO.builder()
                .id(message.getId())
                .senderUUID(message.getSenderUUID())
                .reaction(message.getReaction())
                .status(message.getStatus())
                .message(message.getMessage())
                .reactions(message.getReactions().stream()
                        .map(GroupMessageReaction::getReaction)
                        .collect(Collectors.toSet()))
                .replayTo(Objects.nonNull(message.getReplayTo()) ?
                        mapToMessageVOWithoutNesting(message.getReplayTo()) : null)
                .createdAt(message.getCreatedAt())
                .build();
    }

    private static MessageVO mapToMessageVOWithoutNesting(Message message) {
        return MessageVO.builder()
                .id(message.getId())
                .senderUUID(message.getSenderUUID())
                .reaction(message.getReaction())
                .status(message.getStatus())
                .message(message.getMessage())
                .reactions(message.getReactions().stream()
                        .map(GroupMessageReaction::getReaction)
                        .collect(Collectors.toSet()))
                .replayTo(null)
                .createdAt(message.getCreatedAt())
                .build();
    }

    public static ConversationVO mapToConversationVO(Long userId, Conversations conversation) {
        return ConversationVO.builder()
                .id(conversation.getId())
                .groupName(Objects.equals(conversation.getType(), ConversationType.GROUP_CHAT) ?
                        conversation.getConversationName() : null)
                .type(conversation.getType())
                .build();
    }
}
