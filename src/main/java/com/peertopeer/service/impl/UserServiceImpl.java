package com.peertopeer.service.impl;

import com.peertopeer.vo.UserVO;
import com.peertopeer.entity.Users;
import com.peertopeer.vo.UserLoginVO;
import lombok.RequiredArgsConstructor;
import com.peertopeer.service.JwtService;
import com.peertopeer.service.UserService;
import com.peertopeer.entity.Conversations;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import com.peertopeer.repository.UserRepository;
import com.peertopeer.repository.ConversationsRepository;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final JwtService jwtService;
    private final PasswordEncoder encoder;
    private final UserRepository userRepository;
    private final AuthenticationManager authenticationManager;
    private final ConversationsRepository conversationsRepository;

    @Override
    public String login(UserLoginVO request) {
        Authentication authenticate = authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(request.getUsername(),
                request.getPassword()));
        Users users = userRepository.findByUsername(request.getUsername()).orElseThrow();
        SecurityContextHolder.getContext().setAuthentication(authenticate);
        return jwtService.generateToken(users);
    }

    public List<Conversations> userConversations(Long currentUserId) {
        List<Conversations> conversations = conversationsRepository.findByUsers_Id(currentUserId);

        return null;
    }

    @Override
    public List<UserVO> userList(Long currentUserId) {
        return userRepository.findAll()
                .stream().map(users -> {
                    Optional<Long> byUsersIdAndUsersId = conversationsRepository.findByUsers_IdAndUsers_Id(currentUserId, users.getId());
                    return UserVO.builder()
                            .id(users.getId())
                            .username(users.getUsername())
                            .conversationId(byUsersIdAndUsersId.orElse(null))
                            .build();
                }).toList();
    }

    @Override
    public UserVO getUser(String username) {
        return userRepository.findByUsername(username)
                .map(user -> UserVO.builder()
                        .id(user.getId())
                        .username(username)
                        .build())
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    @Override
    public void register(UserVO user) {
        userRepository.save(Users.builder()
                .username(user.getUsername())
                .password(encoder.encode(user.getPassword()))
                .build());
    }


}
