package org.example.besmarthelpdesk.facade.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.besmarthelpdesk.dto.request.RegisterRequest;
import org.example.besmarthelpdesk.dto.response.MemberResponse;
import org.example.besmarthelpdesk.facade.MemberFacade;
import org.example.besmarthelpdesk.service.MemberService;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class MemberFacadeImpl implements MemberFacade {

    private final MemberService memberService;

    @Override
    public MemberResponse register(RegisterRequest request) {
        log.info("(register) request: {}", request);
        return memberService.createMember(request);
    }

    @Override
    public MemberResponse getMember(UUID id) {
        log.info("(getMember) id: {}", id);
        return memberService.getMemberById(id);
    }

    @Override
    public List<MemberResponse> getAllMembers() {
        log.info("(getAllMembers)");
        return memberService.getAllMembers();
    }
}
