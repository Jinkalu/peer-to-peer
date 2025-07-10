package com.peertopeer.controller;


import com.peertopeer.entity.Message;
import lombok.RequiredArgsConstructor;
import com.peertopeer.service.ChatService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;

    @ResponseBody
    @GetMapping("/chat-history")
    public List<Message> getChatHistory(@RequestParam String conversationId) {
        return chatService.getChatHistory(conversationId);
    }

}
