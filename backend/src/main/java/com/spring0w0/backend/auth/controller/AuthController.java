package com.spring0w0.backend.auth.controller;

import com.spring0w0.backend.auth.dto.LoginRequest;
import com.spring0w0.backend.auth.dto.LoginResponse;
import com.spring0w0.backend.auth.service.JwtTokenService;
import com.spring0w0.backend.common.Result;
import com.spring0w0.backend.user.entity.User;
import com.spring0w0.backend.user.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;
    private final JwtTokenService jwtTokenService;

    @PostMapping("/login")
    public Result<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        User user = userService.authenticate(request.username(), request.password());
        String accessToken = jwtTokenService.createAccessToken(user.getUsername(), user.getRole());
        return Result.success(new LoginResponse(accessToken));
    }
}
