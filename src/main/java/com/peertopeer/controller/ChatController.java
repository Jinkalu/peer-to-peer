package com.peertopeer.controller;


import com.peertopeer.entity.Conversations;
import com.peertopeer.entity.Message;
import com.peertopeer.vo.GroupCreationVO;
import lombok.RequiredArgsConstructor;
import com.peertopeer.service.ChatService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;

    @GetMapping("/chat-history")
    public List<Message> getChatHistory(@RequestParam String conversationId) {
        return chatService.getChatHistory(conversationId);
    }

    @GetMapping("/create-group")
    public Conversations createGroup(@RequestBody GroupCreationVO requestBody) {
        return chatService.createGroup(requestBody);
    }

}
