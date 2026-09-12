package org.example.besmarthelpdesk.service;

import org.example.besmarthelpdesk.dto.response.AlertResponse;
import org.example.besmarthelpdesk.enums.AlertType;

import java.util.List;
import java.util.UUID;

public interface AlertService {
    AlertResponse createAlert(UUID requestId, UUID targetMemberId, AlertType alertType, String message);
    void notifyAdmins(UUID requestId, AlertType alertType, String message);
    List<AlertResponse> getAlertsForMember(UUID memberId, Boolean unreadOnly);
    AlertResponse markAsRead(UUID alertId, UUID memberId);
}
