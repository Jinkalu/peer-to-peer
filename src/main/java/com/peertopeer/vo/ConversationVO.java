package com.peertopeer.vo;

import com.peertopeer.enums.ConversationStatus;
import com.peertopeer.enums.ConversationType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConversationVO {
    private Long id;
    private String conversationName;
    private ConversationType type;
    private ConversationStatus status;
    private boolean readStatus;
    private boolean isPined;
    private List<MessageVO> messages;
    private ChatUserVO receiverDetails;
    private Long createdAt;
    private Long updatedAt;
}
