package com.wordflow.common;

/**
 * 统一返回码。
 */
public enum ResultCode {

    /** 请求成功 */
    SUCCESS(200, "成功"),
    /** 请求参数错误 */
    BAD_REQUEST(400, "请求参数错误"),
    /** 未登录或登录已过期 */
    UNAUTHORIZED(401, "未登录或登录已过期"),
    /** 没有权限 */
    FORBIDDEN(403, "没有权限"),
    /** 资源不存在 */
    NOT_FOUND(404, "资源不存在"),
    /** 资源冲突（如用户名已占用） */
    CONFLICT(409, "资源冲突"),
    /** 服务器内部错误 */
    INTERNAL_ERROR(500, "服务器内部错误");

    private final int code;
    private final String message;

    ResultCode(int code, String message) {
        this.code = code;
        this.message = message;
    }

    public int getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }
}
