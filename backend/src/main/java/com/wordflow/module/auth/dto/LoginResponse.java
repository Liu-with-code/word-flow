package com.wordflow.module.auth.dto;

import com.wordflow.module.user.dto.UserVO;

/**
 * 登录成功响应：令牌 + 用户信息。
 */
public record LoginResponse(String token, UserVO user) {
}

