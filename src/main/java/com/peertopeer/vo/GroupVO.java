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
public class GroupVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 2161538726226613064L;

    private ConversationVO groupDetails;
    private List<UserVO> members;

}
