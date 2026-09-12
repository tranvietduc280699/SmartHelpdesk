package org.example.besmarthelpdesk.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.besmarthelpdesk.constant.MessageConstants;
import org.example.besmarthelpdesk.dto.response.AlertResponse;
import org.example.besmarthelpdesk.entity.Alert;
import org.example.besmarthelpdesk.entity.Member;
import org.example.besmarthelpdesk.enums.AlertType;
import org.example.besmarthelpdesk.enums.Role;
import org.example.besmarthelpdesk.exception.BadRequestException;
import org.example.besmarthelpdesk.exception.ResourceNotFoundException;
import org.example.besmarthelpdesk.repository.AlertRepository;
import org.example.besmarthelpdesk.repository.MemberRepository;
import org.example.besmarthelpdesk.service.AlertService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AlertServiceImpl implements AlertService {

    private final AlertRepository alertRepository;
    private final MemberRepository memberRepository;

    @Override
    @Transactional
    public AlertResponse createAlert(UUID requestId, UUID targetMemberId, AlertType alertType, String message) {
        log.info("(createAlert) requestId: {}, targetMemberId: {}, alertType: {}", requestId, targetMemberId, alertType);

        Alert alert = Alert.builder()
                .requestId(requestId)
                .targetMemberId(targetMemberId)
                .alertType(alertType)
                .message(message)
                .isRead(false)
                .createdAt(Instant.now())
                .build();

        Alert saved = alertRepository.save(alert);
        log.info("(createAlert) saved alert ID: {}", saved.getId());
        return mapToResponse(saved);
    }

    @Override
    @Transactional
    public void notifyAdmins(UUID requestId, AlertType alertType, String message) {
        log.info("(notifyAdmins) requestId: {}, alertType: {}", requestId, alertType);

        List<Member> admins = memberRepository.findByRoleAndIsDeletedFalse(Role.ADMIN);
        if (admins.isEmpty()) {
            log.warn("(notifyAdmins) no active admin found to notify");
            return;
        }

        List<Alert> alerts = admins.stream()
                .map(admin -> Alert.builder()
                        .requestId(requestId)
                        .targetMemberId(admin.getId())
                        .alertType(alertType)
                        .message(message)
                        .isRead(false)
                        .createdAt(Instant.now())
                        .build())
                .collect(Collectors.toList());

        alertRepository.saveAll(alerts);
        log.info("(notifyAdmins) successfully generated {} alerts for admins", alerts.size());
    }

    @Override
    @Transactional(readOnly = true)
    public List<AlertResponse> getAlertsForMember(UUID memberId, Boolean unreadOnly) {
        log.info("(getAlertsForMember) memberId: {}, unreadOnly: {}", memberId, unreadOnly);

        List<Alert> alerts;
        if (Boolean.TRUE.equals(unreadOnly)) {
            alerts = alertRepository.findByTargetMemberIdAndIsReadFalseOrderByCreatedAtDesc(memberId);
        } else {
            alerts = alertRepository.findByTargetMemberIdOrderByCreatedAtDesc(memberId);
        }

        return alerts.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public AlertResponse markAsRead(UUID alertId, UUID memberId) {
        log.info("(markAsRead) alertId: {}, memberId: {}", alertId, memberId);

        Alert alert = alertRepository.findById(alertId)
                .orElseThrow(() -> {
                    log.warn("(markAsRead) alert with ID {} not found", alertId);
                    return new ResourceNotFoundException(MessageConstants.ALERT_NOT_FOUND + alertId);
                });

        if (!alert.getTargetMemberId().equals(memberId)) {
            log.warn("(markAsRead) member {} is not authorized to read alert {}", memberId, alertId);
            throw new BadRequestException(MessageConstants.ACCESS_DENIED);
        }

        alert.setIsRead(true);
        Alert updated = alertRepository.save(alert);
        log.info("(markAsRead) alert {} marked as read", alertId);
        return mapToResponse(updated);
    }

    private AlertResponse mapToResponse(Alert alert) {
        return AlertResponse.builder()
                .id(alert.getId())
                .requestId(alert.getRequestId())
                .targetMemberId(alert.getTargetMemberId())
                .alertType(alert.getAlertType())
                .message(alert.getMessage())
                .isRead(alert.getIsRead())
                .createdAt(alert.getCreatedAt())
                .build();
    }
}
