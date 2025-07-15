package com.peertopeer.controller;

import com.peertopeer.service.UserService;
import com.peertopeer.vo.UserVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/user")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;


    @GetMapping("/{currentUserId}")
    public List<UserVO> listUsers(@PathVariable Long currentUserId) {
        return userService.userList(currentUserId);

    }

    @GetMapping("/by-username")
    public  UserVO getUser(@RequestParam String username) {
        return userService.getUser(username);

    }
}
