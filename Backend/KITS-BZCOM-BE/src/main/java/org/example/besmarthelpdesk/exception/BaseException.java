package org.example.besmarthelpdesk.exception;

import lombok.Getter;
import org.example.besmarthelpdesk.enums.ErrorCode;

@Getter
public class BaseException extends RuntimeException {
    private final ErrorCode errorCode;
    private final Object[] params;

    public BaseException(ErrorCode errorCode, Object... params) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
        this.params = params;
    }

    public BaseException(String message, ErrorCode errorCode, Object... params) {
        super(message);
        this.errorCode = errorCode;
        this.params = params;
    }
}
