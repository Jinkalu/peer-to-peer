package com.peertopeer.vo;

import com.peertopeer.entity.Users;
import com.peertopeer.enums.ConversationStatus;
import com.peertopeer.enums.ConversationType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConversationVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 7290680791240176128L;

    private Long id;
    private UserVO owner;
    private String groupName;
    private Long unreadCount;
    private ConversationType type;
    private ConversationStatus status;
    private UserVO peerUser;
    private List<UserVO> members;
    private Boolean isPined;
    private Boolean readStatus;
    private Long createdAt;
    private Long updatedAt;
}
