package org.example.besmarthelpdesk.enums;

import lombok.Getter;
import org.example.besmarthelpdesk.constant.MessageConstants;
import org.springframework.http.HttpStatus;

@Getter
public enum ErrorCode {
    AUTH_FORBIDDEN(HttpStatus.FORBIDDEN, "AUTH_403", MessageConstants.ACCESS_DENIED),
    AUTH_UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "AUTH_401", MessageConstants.UNAUTHORIZED),
    VALIDATION_INVALID_INPUT(HttpStatus.BAD_REQUEST, "VAL_400", MessageConstants.VALIDATION_ERROR),
    SYSTEM_INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "SYS_500", MessageConstants.SYSTEM_ERROR),
    RESOURCE_NOT_FOUND(HttpStatus.NOT_FOUND, "RES_404", MessageConstants.NOT_FOUND),
    BAD_REQUEST(HttpStatus.BAD_REQUEST, "BAD_400", MessageConstants.BAD_REQUEST);

    private final HttpStatus status;
    private final String code;
    private final String message;

    ErrorCode(HttpStatus status, String code, String message) {
        this.status = status;
        this.code = code;
        this.message = message;
    }
}
