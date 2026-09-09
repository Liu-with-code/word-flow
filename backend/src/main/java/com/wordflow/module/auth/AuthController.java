package com.wordflow.module.auth;

import com.wordflow.common.Result;
import com.wordflow.module.auth.dto.LoginRequest;
import com.wordflow.module.auth.dto.LoginResponse;
import com.wordflow.module.auth.dto.RegisterRequest;
import com.wordflow.module.user.dto.UserVO;
import com.wordflow.module.user.entity.User;
import com.wordflow.module.user.service.UserService;
import com.wordflow.security.JwtUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 认证接口：注册 / 登录。
 *
 * 模块职责：
 *   - 注册成功后直接签发 JWT，登录成功后返回 JWT 与用户信息。
 * 你需要完成：
 *   - 验证码、第三方登录（微信/QQ）可在此模块扩展。
 */
@Tag(name = "认证")
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;
    private final JwtUtil jwtUtil;

    @Operation(summary = "注册")
    @PostMapping("/register")
    public Result<LoginResponse> register(@Valid @RequestBody RegisterRequest request) {
        User user = userService.register(request.username(), request.password(), request.nickname());
        return Result.ok(buildLoginResponse(user));
    }

    @Operation(summary = "登录")
    @PostMapping("/login")
    public Result<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        User user = userService.login(request.username(), request.password());
        return Result.ok(buildLoginResponse(user));
    }

    private LoginResponse buildLoginResponse(User user) {
        UserVO userVO = userService.toVO(user);
        return new LoginResponse(jwtUtil.generateToken(user.getId()), userVO);
    }
}

