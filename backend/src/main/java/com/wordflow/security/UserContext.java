package com.wordflow.security;

/**
 * 当前登录用户上下文（ThreadLocal）。
 *
 * 模块职责：
 *   - 认证拦截器解析 JWT 后将 userId 放入上下文，业务层通过
 *     UserContext.getUserId() 获取当前用户，避免每个接口手动传参。
 * 你需要完成：
 *   - 若后期需要角色/权限，可扩展为 UserContext.set(userId, roles) 结构。
 */
public final class UserContext {

    private static final ThreadLocal<Long> USER_ID = new ThreadLocal<>();

    private UserContext() {
    }

    public static void setUserId(Long userId) {
        USER_ID.set(userId);
    }

    public static Long getUserId() {
        Long userId = USER_ID.get();
        if (userId == null) {
            throw new IllegalStateException("当前线程未登录");
        }
        return userId;
    }

    public static void clear() {
        USER_ID.remove();
    }
}

