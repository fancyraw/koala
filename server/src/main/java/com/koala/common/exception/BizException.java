package com.koala.common.exception;

import com.koala.common.result.ErrorCode;
import lombok.Getter;

/**
 * 业务异常：携带可向前端展示的文案。全局处理器转为统一响应体。
 */
@Getter
public class BizException extends RuntimeException {

    private final int code;

    public BizException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.code = errorCode.getCode();
    }

    public BizException(ErrorCode errorCode, String message) {
        super(message);
        this.code = errorCode.getCode();
    }

    public BizException(int code, String message) {
        super(message);
        this.code = code;
    }
}
