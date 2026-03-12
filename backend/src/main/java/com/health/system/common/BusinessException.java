package com.health.system.common;

public class BusinessException extends RuntimeException {

    private final ErrorCode errorCode;

    public BusinessException(String message) {
        this(ErrorCode.INTERNAL_ERROR, message);
    }

    public BusinessException(int code, String message) {
        this(resolveCode(code), message);
    }

    public BusinessException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public static BusinessException badRequest(String message) {
        return new BusinessException(ErrorCode.BAD_REQUEST, message);
    }

    public static BusinessException unauthorized(String message) {
        return new BusinessException(ErrorCode.UNAUTHORIZED, message);
    }

    public static BusinessException forbidden(String message) {
        return new BusinessException(ErrorCode.FORBIDDEN, message);
    }

    public static BusinessException notFound(String message) {
        return new BusinessException(ErrorCode.NOT_FOUND, message);
    }

    public static BusinessException conflict(String message) {
        return new BusinessException(ErrorCode.CONFLICT, message);
    }

    public int getCode() {
        return errorCode.code();
    }

    public ErrorCode getErrorCode() {
        return errorCode;
    }

    private static ErrorCode resolveCode(int code) {
        for (ErrorCode item : ErrorCode.values()) {
            if (item.code() == code) {
                return item;
            }
        }
        return ErrorCode.INTERNAL_ERROR;
    }
}