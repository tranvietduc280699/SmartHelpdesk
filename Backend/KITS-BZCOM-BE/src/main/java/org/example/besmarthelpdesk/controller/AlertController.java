package org.example.besmarthelpdesk.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.besmarthelpdesk.constant.MessageConstants;
import org.example.besmarthelpdesk.dto.ResponseGeneral;
import org.example.besmarthelpdesk.dto.response.AlertResponse;
import org.example.besmarthelpdesk.facade.AlertFacade;
import org.example.besmarthelpdesk.security.UserPrincipal;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/alerts")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Alerts", description = "Endpoints for Notification / Alert management")
public class AlertController {

    private final AlertFacade alertFacade;

    @GetMapping
    @Operation(summary = "Get My Alerts", description = "Retrieve notifications for the authenticated user, optionally filter by unread only")
    public ResponseEntity<ResponseGeneral<List<AlertResponse>>> getMyAlerts(
            @RequestParam(required = false, defaultValue = "false") Boolean unreadOnly,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        log.info("(getMyAlerts) user: {}, unreadOnly: {}", currentUser.getUsername(), unreadOnly);
        List<AlertResponse> responses = alertFacade.getAlerts(currentUser, unreadOnly);
        return ResponseEntity.ok(ResponseGeneral.success(responses));
    }

    @PatchMapping("/{id}/read")
    @Operation(summary = "Mark Alert as Read", description = "Mark a notification as read (only the target member can mark their own alert)")
    public ResponseEntity<ResponseGeneral<AlertResponse>> markAlertAsRead(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        log.info("(markAlertAsRead) alertId: {}, user: {}", id, currentUser.getUsername());
        AlertResponse response = alertFacade.markAsRead(id, currentUser);
        return ResponseEntity.ok(ResponseGeneral.of(200, MessageConstants.ALERT_READ_SUCCESS, response));
    }
}
