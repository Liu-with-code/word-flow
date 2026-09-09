package com.wordflow.common;

import lombok.Getter;

/**
 * 业务异常。
 *
 * 模块职责：
 *   - 业务逻辑中主动抛出的异常，会被全局异常处理器转换为统一响应。
 * 使用示例：throw new BusinessException(ResultCode.NOT_FOUND, "单词不存在");
 * 你需要完成：
 *   - 无需修改；直接使用即可。
 */
@Getter
public class BusinessException extends RuntimeException {

    private final int code;

    public BusinessException(ResultCode resultCode) {
        super(resultCode.getMessage());
        this.code = resultCode.getCode();
    }

    public BusinessException(ResultCode resultCode, String message) {
        super(message);
        this.code = resultCode.getCode();
    }

    public BusinessException(int code, String message) {
        super(message);
        this.code = code;
    }
}

