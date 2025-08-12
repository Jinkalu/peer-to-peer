package com.peertopeer.controller;


import com.peertopeer.vo.MessageVO;
import lombok.RequiredArgsConstructor;
import com.peertopeer.vo.ConversationVO;
import com.peertopeer.service.ChatService;
import org.springframework.web.bind.annotation.*;
import com.peertopeer.service.ConversationService;

import java.util.List;

@RestController
@RequestMapping("/api/conversation")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;
    private final ConversationService conversationService;

    @PostMapping("/create/{peerUserId}")
    public Long createConversation(@PathVariable Long peerUserId) {
        return conversationService.createConversation(peerUserId);
    }

    @GetMapping("/chat-history")
    public List<MessageVO> getChatHistory(@RequestParam String conversationId) {
        return chatService.getChatHistory(conversationId);
    }

    @GetMapping("/conversation-list")
    public List<ConversationVO> listConversations() {
        return conversationService.listConversations();
    }

    @PostMapping("/delete/{messageId}")
    public void deleteMessage(@PathVariable Long messageId) {
        chatService.deleteMessage(messageId);
    }


    @PostMapping("/reaction/{messageId}")
    public void messageReaction(@PathVariable Long messageId,
                                @RequestParam(required = false) String type,
                                @RequestParam(required = false) String reaction) {
        chatService.messageReaction(messageId, reaction, type);
    }
}
