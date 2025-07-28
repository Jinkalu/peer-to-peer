package com.peertopeer.utils;

import com.peertopeer.entity.Conversations;
import com.peertopeer.entity.Message;
import com.peertopeer.enums.ConversationType;
import com.peertopeer.vo.ConversationVO;
import com.peertopeer.vo.MessageVO;
import com.peertopeer.vo.UserVO;
import com.peertopeer.entity.Users;

import java.util.List;
import java.util.Objects;


public class ChatUtils {

    public static UserVO mapToMembersVO(Users member) {
        return UserVO.builder()
                .id(member.getId())
                .username(member.getUsername())
                .build();
    }

    public static MessageVO mapToMessageVO(Message message){
        return MessageVO.builder()
                .id(message.getId())
                .senderUUID(message.getSenderUUID())
                .status(message.getStatus())
                .message(message.getMessage())
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
