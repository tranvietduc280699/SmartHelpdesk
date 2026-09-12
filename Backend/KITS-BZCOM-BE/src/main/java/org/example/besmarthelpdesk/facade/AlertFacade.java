package org.example.besmarthelpdesk.facade;

import org.example.besmarthelpdesk.dto.response.AlertResponse;
import org.example.besmarthelpdesk.security.UserPrincipal;

import java.util.List;
import java.util.UUID;

public interface AlertFacade {
    List<AlertResponse> getAlerts(UserPrincipal currentUser, Boolean unreadOnly);
    AlertResponse markAsRead(UUID alertId, UserPrincipal currentUser);
}
