package com.peertopeer.service;

import com.peertopeer.vo.ConversationVO;

import java.util.List;

public interface ConversationService {

    List<ConversationVO> listConversations(Long userId);

    Long unreadCountInConvo(Long currentUserUid, Long conversationId);

    String findPeerUser(Long aLong, Long aLong1);

    Long createConversation(Long peerUserId);
}
