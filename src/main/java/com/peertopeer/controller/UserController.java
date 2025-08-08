package com.peertopeer.controller;

import com.peertopeer.service.UserService;
import com.peertopeer.vo.UserLoginVO;
import com.peertopeer.vo.UserVO;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/user")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody UserVO user) {
        userService.register(user);
        return ResponseEntity.ok("User registered");
    }

    @PostMapping("/log-in")
    public ResponseEntity<String> login(@RequestBody UserLoginVO user) {
        return ResponseEntity.ok(userService.login(user));
    }


    @GetMapping("/{currentUserId}")
    public List<UserVO> listUsers(@PathVariable Long currentUserId) {
        return userService.userList(currentUserId);

    }

    @GetMapping("/by-username")
    public UserVO getUser(@RequestParam String username) {
        return userService.getUser(username);

    }
}
