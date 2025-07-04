package com.peertopeer.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatUserVO {
    private Long id;
    private String userUUID;
    private String firstName;
    private String lastName;
    private String userName;
    private String status;
    private String profilePictureUrl;
    private Long conversationId;
    private Boolean isVerified;
    private String customBadge;
    private String accountType;
    private Boolean isBlocked;
    private Boolean isPrivate;
}
