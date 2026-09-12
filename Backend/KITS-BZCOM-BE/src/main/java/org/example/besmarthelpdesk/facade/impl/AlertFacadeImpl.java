package org.example.besmarthelpdesk.facade.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.besmarthelpdesk.dto.response.AlertResponse;
import org.example.besmarthelpdesk.facade.AlertFacade;
import org.example.besmarthelpdesk.security.UserPrincipal;
import org.example.besmarthelpdesk.service.AlertService;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class AlertFacadeImpl implements AlertFacade {

    private final AlertService alertService;

    @Override
    public List<AlertResponse> getAlerts(UserPrincipal currentUser, Boolean unreadOnly) {
        log.info("(getAlerts) user: {}, unreadOnly: {}", currentUser.getUsername(), unreadOnly);
        return alertService.getAlertsForMember(currentUser.getId(), unreadOnly);
    }

    @Override
    public AlertResponse markAsRead(UUID alertId, UserPrincipal currentUser) {
        log.info("(markAsRead) alertId: {}, user: {}", alertId, currentUser.getUsername());
        return alertService.markAsRead(alertId, currentUser.getId());
    }
}
