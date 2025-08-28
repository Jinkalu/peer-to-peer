package com.peertopeer.vo;

import com.peertopeer.enums.MessageStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.util.Set;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MessageVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 5448499802141865753L;

    private String id;
    private ConversationVO conversationVO;
    private String senderUUID;
    private String senderUsername;
    private String receiverUUID;
    private String message;
    private String mediaURL;
    private String reaction;
    private Integer sourceId;
    private String sourceType;
    private MessageStatus status;
    private Set<String> reactions;
    private Long createdAt;
    private MessageVO replayTo;
    private Long updateAt;
}
