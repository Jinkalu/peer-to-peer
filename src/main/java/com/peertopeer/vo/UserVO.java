package com.peertopeer.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserVO implements Serializable {

    @Serial
    private static final long serialVersionUID = -6758910583263958657L;

    private Long id;
    private Boolean isOwner;
    private String username;
    private String password;
    private Long conversationId;

}
