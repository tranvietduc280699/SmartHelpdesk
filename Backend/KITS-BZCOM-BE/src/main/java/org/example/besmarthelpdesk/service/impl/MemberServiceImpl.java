package org.example.besmarthelpdesk.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import com.fasterxml.uuid.Generators;
import org.example.besmarthelpdesk.constant.MessageConstants;
import org.example.besmarthelpdesk.dto.request.RegisterRequest;
import org.example.besmarthelpdesk.dto.response.MemberResponse;
import org.example.besmarthelpdesk.entity.Member;
import org.example.besmarthelpdesk.exception.BadRequestException;
import org.example.besmarthelpdesk.exception.ResourceNotFoundException;
import org.example.besmarthelpdesk.entity.Company;
import org.example.besmarthelpdesk.repository.CompanyRepository;
import org.example.besmarthelpdesk.repository.MemberRepository;
import org.example.besmarthelpdesk.service.MemberService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import java.util.Map;
import java.util.Objects;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class MemberServiceImpl implements MemberService {

    private final MemberRepository memberRepository;
    private final CompanyRepository companyRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public MemberResponse createMember(RegisterRequest request) {
        log.info("(createMember) request: {}", request);

        if (memberRepository.existsByEmail(request.getEmail())) {
            log.warn("(createMember) email {} already exists", request.getEmail());
            throw new BadRequestException(MessageConstants.EMAIL_REGISTERED);
        }

        String companyId = null;
        if (request.getCompanyId() != null && !request.getCompanyId().trim().isEmpty()) {
            companyId = request.getCompanyId().trim();
            if (!companyRepository.existsById(companyId)) {
                log.warn("(createMember) company with ID {} not found", companyId);
                throw new ResourceNotFoundException(MessageConstants.COMPANY_NOT_FOUND + companyId);
            }
        }

        UUID memberId = Generators.timeBasedEpochGenerator().generate();

        Member member = Member.builder()
            .id(memberId)
            .createdBy(memberId)
            .updatedBy(memberId)
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .name(request.getName())
                .role(request.getRole())
                .companyId(companyId)
                .phone(request.getPhone())
                .status("active")
                .isDeleted(false)
                .build();

        Member savedMember = memberRepository.save(member);
        log.info("(createMember) savedMember ID: {}", savedMember.getId());

        return mapToResponse(savedMember);
    }

    @Override
    @Transactional(readOnly = true)
    public MemberResponse getMemberById(UUID id) {
        log.info("(getMemberById) id: {}", id);
        Member member = memberRepository.findById(id)
                .orElseThrow(() -> {
                    log.warn("(getMemberById) member with ID {} not found", id);
                    return new ResourceNotFoundException(MessageConstants.MEMBER_NOT_FOUND + id);
                });
        return mapToResponse(member);
    }

    @Override
    @Transactional(readOnly = true)
    public List<MemberResponse> getAllMembers() {
        log.info("(getAllMembers)");
        List<Member> members = memberRepository.findAll();

        Set<String> companyIds = members.stream()
                .map(Member::getCompanyId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        Map<String, String> companyNameMap = companyRepository.findAllById(companyIds).stream()
                .collect(Collectors.toMap(Company::getId, Company::getCompanyName));

        return members.stream()
                .map(member -> mapToResponse(member, companyNameMap.get(member.getCompanyId())))
                .collect(Collectors.toList());
    }

    private MemberResponse mapToResponse(Member member) {
        String companyName = null;
        if (member.getCompanyId() != null) {
            companyName = companyRepository.findById(member.getCompanyId())
                    .map(Company::getCompanyName)
                    .orElse(null);
        }
        return mapToResponse(member, companyName);
    }

    private MemberResponse mapToResponse(Member member, String companyName) {
        return MemberResponse.builder()
                .id(member.getId())
                .email(member.getEmail())
                .name(member.getName())
                .role(member.getRole())
                .phone(member.getPhone())
                .companyId(member.getCompanyId())
                .companyName(companyName)
                .status(member.getStatus())
                .isDeleted(member.getIsDeleted())
                .createdAt(member.getCreatedAt())
                .build();
    }
}
