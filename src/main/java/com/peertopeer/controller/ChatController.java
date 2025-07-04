package com.peertopeer.controller;

import com.peertopeer.entity.Message;
import com.peertopeer.service.ChatService;
import com.peertopeer.service.UserService;
import com.peertopeer.vo.UserLoginVO;
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
    private final UserService userService;


    @PostMapping("/api/login")
    public ResponseEntity<Long> login(@RequestBody UserLoginVO request) {
        return ResponseEntity.ok(10L);
    }

    @ResponseBody
    @GetMapping("/api/users")
    public static List<String> listUsers() {
        return List.of("u1", "u2", "u3", "u4", "u5");

    }

    @ResponseBody
    @GetMapping("/api/chat-history")
    public List<Message> getChatHistory(String sender, String receiver) {
        return chatService.getChatHistory(sender, receiver);
    }

    @PostMapping("/api/mark-seen")
    public ResponseEntity<Void> markMessagesSeen(@RequestParam String sender, @RequestParam String receiver) {
        chatService.markMessagesAsSeen(sender, receiver);
        return ResponseEntity.ok().build();
    }


    public static void main(String[] args) {



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

