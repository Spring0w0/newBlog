package com.spring0w0.backend.auth.config;

import com.spring0w0.backend.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.stereotype.Component;

/**
 * 在 Web 服务开始接收请求前，确保空数据库拥有且只拥有一个由环境配置提供的初始管理员。
 */
@Component
@RequiredArgsConstructor
public class InitialAdminBootstrap implements SmartInitializingSingleton {

    private final UserService userService;
    private final InitialAdminProperties initialAdminProperties;

    @Override
    public void afterSingletonsInstantiated() {
        userService.initializeFirstAdministrator(initialAdminProperties.username(), initialAdminProperties.password());
    }
}
