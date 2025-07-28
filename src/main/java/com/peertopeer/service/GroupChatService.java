package com.peertopeer.service;

import com.peertopeer.entity.Message;
import com.peertopeer.vo.GroupVO;
import com.peertopeer.vo.GroupCreationVO;
import org.springframework.web.socket.WebSocketSession;

import java.util.List;
import java.util.Map;

public interface GroupChatService {

    GroupVO createGroup(GroupCreationVO conversationId);

    void addMember(GroupCreationVO conversationId);

    void removeMember(Long conversationId, Long memberId);

    GroupVO membersList(Long conversationId, int page, int size);

    void groupMsg(WebSocketSession session, Map<String, String> payload);


    String getGroupId(WebSocketSession session);

    void removeUserFromRoom(WebSocketSession session);
}