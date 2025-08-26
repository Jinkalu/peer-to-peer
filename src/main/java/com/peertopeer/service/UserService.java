package com.peertopeer.service;

import com.peertopeer.records.UserSummary;
import com.peertopeer.vo.UserLoginVO;
import com.peertopeer.vo.UserVO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface UserService {
    String login(UserLoginVO request);

    List<UserVO> userList(Long currentUserId);

    Page<UserSummary> getUser(String username, Pageable pageable);

    void register(UserVO user);
}
