package com.peertopeer.controller;

import com.peertopeer.entity.Message;
import com.peertopeer.service.ChatService;
import com.peertopeer.service.UserService;
import com.peertopeer.vo.UserLoginVO;
import com.peertopeer.vo.UserVO;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;
import java.util.Scanner;

@Controller
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;


    @PostMapping("/api/login")
    public ResponseEntity<Long> login(@RequestBody UserLoginVO request) {
        return ResponseEntity.ok(10L);
    }


    @ResponseBody
    @GetMapping("/api/chat-history")
    public List<Message> getChatHistory(@RequestParam String conversationId) {
        return chatService.getChatHistory(conversationId);
    }


    /// -pages- ///


    @GetMapping("/user-list")
    public String userList() {
        return "user-list";
    }

    @GetMapping("/private")
    public String chat() {
        return "private-chat";
    }

    @GetMapping("/group")
    public String group() {
        return "group-chat";
    }
}

