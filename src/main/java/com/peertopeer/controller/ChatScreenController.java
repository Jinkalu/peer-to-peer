package com.peertopeer.controller;


import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
@RequiredArgsConstructor
public class ChatScreenController {



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

