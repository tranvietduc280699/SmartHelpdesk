package org.example.besmarthelpdesk.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.besmarthelpdesk.constant.MessageConstants;
import org.example.besmarthelpdesk.dto.ResponseGeneral;
import org.example.besmarthelpdesk.dto.request.RegisterRequest;
import org.example.besmarthelpdesk.dto.response.MemberResponse;
import org.example.besmarthelpdesk.facade.MemberFacade;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/members")
@RequiredArgsConstructor
@Slf4j
public class MemberController {

    private final MemberFacade memberFacade;

    @PostMapping
    public ResponseEntity<ResponseGeneral<MemberResponse>> registerMember(@Valid @RequestBody RegisterRequest request) {
        log.info("(registerMember) request: {}", request);
        MemberResponse response = memberFacade.register(request);
        ResponseGeneral<MemberResponse> body = ResponseGeneral.success(MessageConstants.REGISTER_SUCCESS, response);
        return new ResponseEntity<>(body, HttpStatus.CREATED);
    }

    @GetMapping
    public ResponseEntity<ResponseGeneral<List<MemberResponse>>> getAllMembers() {
        log.info("(getAllMembers)");
        List<MemberResponse> responses = memberFacade.getAllMembers();
        ResponseGeneral<List<MemberResponse>> body = ResponseGeneral.success(responses);
        return new ResponseEntity<>(body, HttpStatus.OK);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ResponseGeneral<MemberResponse>> getMemberDetail(@PathVariable UUID id) {
        log.info("(getMemberDetail) id: {}", id);
        MemberResponse response = memberFacade.getMember(id);
        ResponseGeneral<MemberResponse> body = ResponseGeneral.success(response);
        return new ResponseEntity<>(body, HttpStatus.OK);
    }
}
