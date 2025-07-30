package com.peertopeer.vo;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.peertopeer.enums.MessageStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class MessageResponseVO {
    private String conversationId;
    private String sender;
    private String receiver;
    private String messageId;
    private String msg;
    private String user;
    private Long unreadCount;
    private Boolean online;
    private String type;
    private String from;
    private String fromUsername;
    private String statusReceiver;
    private String msgId;
    private MessageStatus status;
    private Object isTyping;
}
