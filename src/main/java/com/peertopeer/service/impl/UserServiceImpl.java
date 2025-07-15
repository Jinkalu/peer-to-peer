package com.peertopeer.service.impl;

import com.peertopeer.repository.ConversationsRepository;
import com.peertopeer.repository.UserRepository;
import com.peertopeer.service.UserService;
import com.peertopeer.vo.UserLoginVO;
import com.peertopeer.vo.UserVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final ConversationsRepository conversationsRepository;

    @Override
    public Long login(UserLoginVO request) {
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


}
