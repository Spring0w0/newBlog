package com.spring0w0.backend.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.spring0w0.backend.common.ResultCode;
import com.spring0w0.backend.exception.BusinessException;
import com.spring0w0.backend.mapper.UserMapper;
import com.spring0w0.backend.pojo.entity.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
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

    /**
     * 仅当数据库没有任何用户时创建首个管理员，后续重启不会覆盖已修改的密码。
     */
    @Transactional
    public void initializeFirstAdministrator(String username, String password) {
        Long userCount = userMapper.selectCount(Wrappers.emptyWrapper());
        if (userCount != null && userCount > 0) {
            return;
        }

        User administrator = new User();
        administrator.setUsername(username.trim());
        administrator.setPasswordHash(passwordEncoder.encode(password));
        administrator.setRole("ADMIN");
        administrator.setEnabled(true);
        userMapper.insert(administrator);
        log.info("初始化首个管理员账号完成，返回参数：username={}，role=ADMIN", administrator.getUsername());
    }

    private Optional<User> findByUsername(String username) {
        return Optional.ofNullable(userMapper.selectOne(
                Wrappers.<User>lambdaQuery().eq(User::getUsername, username)
        ));
    }
}
