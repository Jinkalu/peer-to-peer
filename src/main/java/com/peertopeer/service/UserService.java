package com.peertopeer.service;

import com.peertopeer.vo.UserLoginVO;

public interface UserService {
    Long login(UserLoginVO request);
}
