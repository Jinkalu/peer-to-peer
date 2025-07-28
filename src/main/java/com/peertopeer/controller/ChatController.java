package com.peertopeer.controller;


import com.peertopeer.entity.Message;
import com.peertopeer.enums.MessageReaction;
import com.peertopeer.service.ConversationService;
import com.peertopeer.vo.ConversationVO;

import com.peertopeer.vo.MessageVO;
import lombok.RequiredArgsConstructor;
import com.peertopeer.service.ChatService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;
    private final ConversationService conversationService;

    @GetMapping("/chat-history")
    public List<MessageVO> getChatHistory(@RequestParam String conversationId) {
        return chatService.getChatHistory(conversationId);
    }

    @GetMapping("conversation/{currentUserId}")
    public List<ConversationVO> listConversations(@PathVariable Long currentUserId) {
        return conversationService.listConversations(currentUserId);
    }

    @PostMapping("/reaction/{messageId}")
    public void messageReaction(@PathVariable Long messageId,
                                @RequestParam(required = false) String reaction) {
        chatService.messageReaction(messageId,reaction);
    }
}
