package com.peertopeer.controller;

import com.peertopeer.vo.UserVO;
import com.peertopeer.vo.UserLoginVO;
import lombok.RequiredArgsConstructor;
import com.peertopeer.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

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
    public ResponseEntity<Map<String,String>> login(@RequestBody UserLoginVO user) {
        return ResponseEntity.ok(Map.of("token", userService.login(user)));
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
