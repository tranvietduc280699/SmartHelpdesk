package org.example.besmarthelpdesk.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.besmarthelpdesk.constant.MessageConstants;
import org.example.besmarthelpdesk.dto.ResponseGeneral;
import org.example.besmarthelpdesk.dto.request.AssignDeveloperDto;
import org.example.besmarthelpdesk.dto.request.ClassifyRequestDto;
import org.example.besmarthelpdesk.dto.request.CreateRequestDto;
import org.example.besmarthelpdesk.dto.request.SuggestPriorityDto;
import org.example.besmarthelpdesk.dto.request.UpdateRequestStatusDto;
import org.example.besmarthelpdesk.dto.response.*;
import org.example.besmarthelpdesk.enums.RequestCategory;
import org.example.besmarthelpdesk.enums.RequestPriority;
import org.example.besmarthelpdesk.enums.RequestStatus;
import org.example.besmarthelpdesk.facade.LlmFacade;
import org.example.besmarthelpdesk.facade.RequestFacade;
import org.example.besmarthelpdesk.security.UserPrincipal;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/requests")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Request Management", description = "Endpoints for Customer Requests, Auto-Assignment, Status, History, and AI features")
public class RequestController {

    private final RequestFacade requestFacade;
    private final LlmFacade llmFacade;

    @PostMapping
    @PreAuthorize("hasRole('CLIENT')")
    @Operation(summary = "Register Request", description = "CLIENT only: Register a new customer request")
    public ResponseEntity<ResponseGeneral<RequestResponse>> registerRequest(
            @Valid @RequestBody CreateRequestDto dto,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        log.info("(registerRequest) user: {}, title: {}", currentUser.getUsername(), dto.getTitle());
        RequestResponse response = requestFacade.createRequest(dto, currentUser);
        return new ResponseEntity<>(ResponseGeneral.of(200, MessageConstants.REQUEST_CREATED, response), HttpStatus.CREATED);
    }

    @GetMapping
    @Operation(summary = "Get Request List", description = "Supports pagination, sorting, composite filtering, and RBAC visibility")
    public ResponseEntity<ResponseGeneral<Page<RequestResponse>>> getRequests(
            @RequestParam(required = false) RequestCategory category,
            @RequestParam(required = false) RequestPriority priority,
            @RequestParam(required = false) RequestStatus status,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        log.info("(getRequests) user: {}, page: {}, size: {}", currentUser.getUsername(), page, size);

        Sort sort = sortDir.equalsIgnoreCase("asc") ? Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, size, sort);

        Page<RequestResponse> responses = requestFacade.getRequests(currentUser, category, priority, status, search, pageable);
        return ResponseEntity.ok(ResponseGeneral.success(responses));
    }

    @GetMapping("/stats")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Request Statistics", description = "ADMIN only: Get completion rate, requests by category, and requests by developer")
    public ResponseEntity<ResponseGeneral<RequestStatsResponse>> getStats(
            @AuthenticationPrincipal UserPrincipal currentUser) {
        log.info("(getStats) user: {}", currentUser.getUsername());
        RequestStatsResponse response = requestFacade.getStats(currentUser);
        return ResponseEntity.ok(ResponseGeneral.success(response));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get Request Detail", description = "Get detailed info including creator, assigned dev, company, and history")
    public ResponseEntity<ResponseGeneral<RequestDetailResponse>> getRequestDetail(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        log.info("(getRequestDetail) id: {}, user: {}", id, currentUser.getUsername());
        RequestDetailResponse response = requestFacade.getRequestDetail(id, currentUser);
        return ResponseEntity.ok(ResponseGeneral.success(response));
    }

    @PatchMapping("/{id}/assign")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Assign Developer", description = "ADMIN only: Manually assign developer or trigger auto-assignment algorithm")
    public ResponseEntity<ResponseGeneral<RequestResponse>> assignDeveloper(
            @PathVariable UUID id,
            @RequestBody(required = false) AssignDeveloperDto dto,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        log.info("(assignDeveloper) requestId: {}, user: {}", id, currentUser.getUsername());
        RequestResponse response = requestFacade.assignDeveloper(id, dto, currentUser);
        return ResponseEntity.ok(ResponseGeneral.of(200, MessageConstants.REQUEST_ASSIGNED, response));
    }

    @PatchMapping("/{id}/status")
    @Operation(summary = "Update Request Status", description = "ADMIN or assigned DEVELOPER: Enforces status transition rules (PENDING -> IN_PROGRESS -> DONE)")
    public ResponseEntity<ResponseGeneral<RequestResponse>> updateStatus(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateRequestStatusDto dto,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        log.info("(updateStatus) requestId: {}, newStatus: {}, user: {}", id, dto.getStatus(), currentUser.getUsername());
        RequestResponse response = requestFacade.updateStatus(id, dto, currentUser);
        return ResponseEntity.ok(ResponseGeneral.of(200, MessageConstants.STATUS_UPDATED, response));
    }

    @GetMapping("/{id}/history")
    @Operation(summary = "Get Request History", description = "Get full chronological history of changes for a request")
    public ResponseEntity<ResponseGeneral<List<RequestHistoryResponse>>> getRequestHistory(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        log.info("(getRequestHistory) id: {}, user: {}", id, currentUser.getUsername());
        List<RequestHistoryResponse> responses = requestFacade.getRequestHistory(id, currentUser);
        return ResponseEntity.ok(ResponseGeneral.success(responses));
    }

    // --- LLM AI Feature Endpoints ---

    @PostMapping("/classify")
    @Operation(summary = "AI Classify Request Category", description = "Automatically classifies request category (BUG, FEATURE, INQUIRY) from description")
    public ResponseEntity<ResponseGeneral<ClassifyResponseDto>> classifyRequest(
            @Valid @RequestBody ClassifyRequestDto dto) {
        log.info("(classifyRequest)");
        ClassifyResponseDto response = llmFacade.classify(dto);
        return ResponseEntity.ok(ResponseGeneral.success(response));
    }

    @PostMapping("/suggest-priority")
    @Operation(summary = "AI Suggest Priority", description = "Automatically suggests priority (HIGH, MEDIUM, LOW) based on request description urgency")
    public ResponseEntity<ResponseGeneral<SuggestPriorityResponseDto>> suggestPriority(
            @Valid @RequestBody SuggestPriorityDto dto) {
        log.info("(suggestPriority)");
        SuggestPriorityResponseDto response = llmFacade.suggestPriority(dto);
        return ResponseEntity.ok(ResponseGeneral.success(response));
    }

    @GetMapping("/{id}/summary")
    @Operation(summary = "AI Request Summary", description = "Automatically generates a 1-2 line summary of a request")
    public ResponseEntity<ResponseGeneral<RequestSummaryResponseDto>> summarizeRequest(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        log.info("(summarizeRequest) id: {}, user: {}", id, currentUser.getUsername());
        RequestSummaryResponseDto response = llmFacade.summarize(id, currentUser);
        return ResponseEntity.ok(ResponseGeneral.success(response));
    }
}
