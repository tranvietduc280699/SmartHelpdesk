package org.example.besmarthelpdesk.exception;

import org.example.besmarthelpdesk.enums.ErrorCode;

public class BadRequestException extends BaseException {
    public BadRequestException(String message) {
        super(message, ErrorCode.BAD_REQUEST);
    }
}
