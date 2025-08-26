package com.peertopeer.controller;

import com.peertopeer.records.UserSummary;
import com.peertopeer.repository.UserRepository;
import com.peertopeer.vo.UserVO;
import com.peertopeer.vo.UserLoginVO;
import lombok.RequiredArgsConstructor;
import com.peertopeer.service.UserService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/user")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final UserRepository userRepository;

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


    @GetMapping("/search")
    public Page<UserSummary> getUser(@RequestParam String username,
                                @RequestParam(defaultValue = "0") int page,
                                @RequestParam(defaultValue = "20") int size) {
        return userService.getUser(username, PageRequest.of(page, size));

    }

  /*  @GetMapping("/by-username")
    public UserVO getUser(@RequestParam String username) {
        return userService.getUser(username);
    }*/


    @GetMapping("/username/{username}")
    public List<UserSummary> byUsername(@PathVariable String username) {
        return userRepository.findByUsernameContaining(username);
    }
}
