package com.peertopeer.service;

import com.peertopeer.vo.UserLoginVO;
import com.peertopeer.vo.UserVO;

import java.util.List;

public interface UserService {
    String login(UserLoginVO request);

    List<UserVO> userList(Long currentUserId);

    UserVO getUser(String username);

    void register(UserVO user);
}
