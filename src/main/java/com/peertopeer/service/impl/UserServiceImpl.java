package com.peertopeer.service.impl;

import com.peertopeer.repository.UserRepository;
import com.peertopeer.service.UserService;
import com.peertopeer.vo.UserLoginVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;

    @Override
    public Long login(UserLoginVO request) {
        return null;
    }


}
