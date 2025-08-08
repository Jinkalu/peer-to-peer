package com.peertopeer.service.impl;

import com.peertopeer.vo.MessageVO;
import com.peertopeer.entity.Message;
import com.peertopeer.utils.ChatUtils;
import lombok.RequiredArgsConstructor;
import jakarta.transaction.Transactional;
import com.peertopeer.service.JwtService;
import com.peertopeer.enums.MessageStatus;
import com.peertopeer.service.ChatService;
import com.peertopeer.entity.Conversations;
import com.peertopeer.enums.ConversationType;
import org.springframework.stereotype.Service;
import com.peertopeer.repository.UserRepository;
import com.peertopeer.entity.GroupMessageReaction;
import com.peertopeer.repository.MessageRepository;
import com.peertopeer.repository.ConversationsRepository;
import com.peertopeer.repository.GroupMessageReactionRepository;

import java.util.List;
import java.util.HashSet;
import java.util.Optional;

import static com.peertopeer.utils.PeerUtils.isValidEmoji;

@Service
@RequiredArgsConstructor
public class ChatServiceImpl implements ChatService {

    private final UserRepository userRepository;
    private final MessageRepository messageRepository;
    private final ConversationsRepository conversationsRepository;
    private final GroupMessageReactionRepository groupMessageReactionRepository;

    @Override
    public List<MessageVO> getChatHistory(String conversationId) {
        List<Message> messages = messageRepository.findByConversation_Id(Long.valueOf(conversationId));
        return messages.stream()
                .map(message -> {
                    MessageVO messageVO = ChatUtils.mapToMessageVO(message);
                    messageVO.setSenderUsername(userRepository.findById(Long.valueOf(message.getSenderUUID()))
                            .get().getUsername());
                    return messageVO;
                }).toList();
    }

    @Override
    public Long create(String user, String target) {
        Long sender = Long.valueOf(user);
        Long receiver = Long.valueOf(target);
        Optional<Long> byUsersIdAndUsersId = conversationsRepository.findByUsers_IdAndUsers_Id(sender, receiver);
        return byUsersIdAndUsersId.orElseGet(() -> conversationsRepository.saveAndFlush(Conversations.builder()
                .users(new HashSet<>(userRepository.findAllById(List.of(sender, receiver))))
                .type(ConversationType.PRIVATE_CHAT)
                .build()).getId());
    }

    @Override
    @Transactional(rollbackOn = Exception.class)
    public Long saveMessage(String conversationId, String fromUser, String msg, MessageStatus status) {
        conversationsRepository.updateUpdatedAtById(System.currentTimeMillis(), Long.valueOf(conversationId));
        Message message = messageRepository.saveAndFlush(Message.builder()
                .message(msg)
                .conversation(conversationsRepository.findById(Long.valueOf(conversationId)).get())
                .senderUUID(fromUser)
                .status(status)
                .build());
        return message.getId();
    }

    @Override
    public void updateMessageStatus(String userId) {
        messageRepository.updateStatusBySenderUUIDAndStatus(MessageStatus.DELIVERED.name(), Long.valueOf(userId),
                MessageStatus.SEND.name());
    }

    @Override
    public void updateMessageChatStatus(long convoId, String user) {
        messageRepository.updateStatusByConversation_IdAndSenderUUIDNotAndStatus(MessageStatus.SEEN, convoId,
                user, MessageStatus.DELIVERED);
    }

    @Override
    public Long unreadCount(String sender, String receiver) {
        return messageRepository.countUnreadMessages(sender, Long.valueOf(receiver));
    }

    @Override
    public void messageReaction(Long messageId, String reaction, String type) {
        if (reaction == null || reaction.isEmpty()) {
            messageRepository.updateReactionById(reaction, messageId);
            return;
        }
        if (!isValidEmoji(reaction)) {
            throw new IllegalArgumentException("Only emojis are allowed!");
        }
        if (type.equals(ConversationType.GROUP_CHAT.name())) {
            groupMessageReactionRepository.save(GroupMessageReaction.builder()
                    .reaction(reaction)
                    .user(JwtService.getUserDetails())
                    .message(messageRepository.findById(messageId).orElseThrow())
                    .build());
        } else {
            messageRepository.updateReactionById(reaction, messageId);
        }
    }

    @Override
    public void deleteMessage(Long messageId) {
        messageRepository.updateStatusById(MessageStatus.DELETED, messageId);
    }


}
