package org.example.besmarthelpdesk.exception;

import org.example.besmarthelpdesk.enums.ErrorCode;

public class ResourceNotFoundException extends BaseException {
    public ResourceNotFoundException(String message) {
        super(message, ErrorCode.RESOURCE_NOT_FOUND);
    }
}
