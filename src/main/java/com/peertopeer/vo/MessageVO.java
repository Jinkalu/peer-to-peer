package com.peertopeer.vo;

import com.peertopeer.enums.MessageStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MessageVO {
    private Long id;
    private String senderUUID;
    private String receiverUUID;
    private String message;
    private String mediaURL;
    private String reaction;
    private Integer sourceId;
    private String sourceType;
//    private StoryRepliesVO storyDetails;
    private MessageStatus status;
    private Long createdAt;
    private Long updateAt;
}
