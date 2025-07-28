package com.peertopeer.vo;

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
public class GroupCreationVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1714334472971467507L;

    private Long userId;
    private Long conversationId;
    private String groupName;
    private List<Long> users;
}
