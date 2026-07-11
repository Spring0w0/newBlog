package com.spring0w0.backend.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.spring0w0.backend.common.ResultCode;
import com.spring0w0.backend.exception.BusinessException;
import com.spring0w0.backend.mapper.UserMapper;
import com.spring0w0.backend.pojo.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;

    public User authenticate(String username, String password) {
        User user = findByUsername(username)
                .orElseThrow(() -> new BusinessException(ResultCode.LOGIN_FAILED));

        if (!Boolean.TRUE.equals(user.getEnabled())) {
            throw new BusinessException(ResultCode.ACCOUNT_DISABLED);
        }

        if (!passwordEncoder.matches(password, user.getPasswordHash())) {
            throw new BusinessException(ResultCode.LOGIN_FAILED);
        }

        return user;
    }

    public Optional<User> findEnabledByUsername(String username) {
        return findByUsername(username).filter(user -> Boolean.TRUE.equals(user.getEnabled()));
    }

    public User getEnabledUser(String username) {
        return findEnabledByUsername(username)
                .orElseThrow(() -> new BusinessException(ResultCode.UNAUTHORIZED));
    }

    private Optional<User> findByUsername(String username) {
        return Optional.ofNullable(userMapper.selectOne(
                Wrappers.<User>lambdaQuery().eq(User::getUsername, username)
        ));
    }
}
