package org.example.besmarthelpdesk.repository;

import org.example.besmarthelpdesk.entity.Member;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface MemberRepository extends JpaRepository<Member, UUID> {
    Optional<Member> findByEmail(String email);
    boolean existsByEmail(String email);
    Optional<Member> findByEmailAndIsDeletedFalse(String email);
    java.util.List<Member> findAllByIsDeletedFalse();
    java.util.List<Member> findByRoleAndIsDeletedFalse(org.example.besmarthelpdesk.enums.Role role);
}
