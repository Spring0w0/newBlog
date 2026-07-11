package com.spring0w0.backend.controller;

import com.spring0w0.backend.common.Result;
import com.spring0w0.backend.pojo.dto.LoginRequest;
import com.spring0w0.backend.pojo.entity.User;
import com.spring0w0.backend.pojo.vo.LoginResponse;
import com.spring0w0.backend.service.JwtTokenService;
import com.spring0w0.backend.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "认证接口", description = "管理员账号密码登录")
public class AuthController {

    private final UserService userService;
    private final JwtTokenService jwtTokenService;

    @PostMapping("/login")
    @Operation(summary = "管理员账号密码登录", description = "验证账号密码后签发 JWT access token；密码不会出现在响应或日志中。")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "登录成功"),
            @ApiResponse(responseCode = "400", description = "用户名或密码不能为空"),
            @ApiResponse(responseCode = "401", description = "用户名或密码错误"),
            @ApiResponse(responseCode = "403", description = "账号已禁用")
    })
    public Result<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        log.info("用户登录，请求参数：username={}，password=已脱敏", request.username());
        User user = userService.authenticate(request.username(), request.password());
        String accessToken = jwtTokenService.createAccessToken(user.getUsername(), user.getRole());
        log.info("用户登录成功，返回参数：userId={}，username={}，role={}", user.getId(), user.getUsername(), user.getRole());
        return Result.success(new LoginResponse(accessToken));
    }
}
