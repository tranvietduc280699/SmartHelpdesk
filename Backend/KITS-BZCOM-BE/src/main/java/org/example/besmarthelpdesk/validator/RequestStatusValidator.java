package org.example.besmarthelpdesk.validator;

import lombok.extern.slf4j.Slf4j;
import org.example.besmarthelpdesk.constant.MessageConstants;
import org.example.besmarthelpdesk.enums.RequestStatus;
import org.example.besmarthelpdesk.exception.BadRequestException;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

@Component
@Slf4j
public class RequestStatusValidator {

    private static final Map<RequestStatus, Set<RequestStatus>> ALLOWED_TRANSITIONS = new EnumMap<>(RequestStatus.class);

    static {
        // PENDING can only move to IN_PROGRESS
        ALLOWED_TRANSITIONS.put(RequestStatus.PENDING, EnumSet.of(RequestStatus.IN_PROGRESS));

        // IN_PROGRESS can only move to DONE
        ALLOWED_TRANSITIONS.put(RequestStatus.IN_PROGRESS, EnumSet.of(RequestStatus.DONE));

        // DONE cannot transition to any other status
        ALLOWED_TRANSITIONS.put(RequestStatus.DONE, EnumSet.noneOf(RequestStatus.class));
    }

    public void validateTransition(RequestStatus from, RequestStatus to) {
        log.info("(validateTransition) validating transition from {} to {}", from, to);

        if (from == null || to == null) {
            log.warn("(validateTransition) status cannot be null: from={}, to={}", from, to);
            throw new BadRequestException(MessageConstants.STATUS_NULL);
        }

        if (from == to) {
            log.warn("(validateTransition) cannot transition to the same status: {}", from);
            throw new BadRequestException(String.format(MessageConstants.INVALID_STATUS_TRANSITION, from, to));
        }

        Set<RequestStatus> allowed = ALLOWED_TRANSITIONS.getOrDefault(from, EnumSet.noneOf(RequestStatus.class));
        if (!allowed.contains(to)) {
            log.warn("(validateTransition) blocked transition: {} -> {}. Allowed: {}", from, to, allowed);
            throw new BadRequestException(String.format(MessageConstants.INVALID_STATUS_TRANSITION, from, to));
        }

        log.info("(validateTransition) transition from {} to {} is valid", from, to);
    }
}
