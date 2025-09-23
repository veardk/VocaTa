package com.vocata.common.exception;

import com.vocata.common.result.ApiCode;

/**
 * 业务异常类 - 用于业务逻辑中的异常抛出
 */
public class BizException extends RuntimeException {

    private Integer code;

    public BizException(String message) {
        super(message);
        this.code = 500;
    }

    public BizException(Integer code, String message) {
        super(message);
        this.code = code;
    }

    public BizException(Integer code, String message, Throwable cause) {
        super(message, cause);
        this.code = code;
    }

    /**
     * 使用ApiCode构造异常
     */
    public BizException(ApiCode apiCode) {
        super(apiCode.getMessage());
        this.code = apiCode.getCode();
    }

    /**
     * 使用ApiCode和自定义消息构造异常
     */
    public BizException(ApiCode apiCode, String customMessage) {
        super(customMessage);
        this.code = apiCode.getCode();
    }

    public Integer getCode() {
        return code;
    }

    public void setCode(Integer code) {
        this.code = code;
    }
}