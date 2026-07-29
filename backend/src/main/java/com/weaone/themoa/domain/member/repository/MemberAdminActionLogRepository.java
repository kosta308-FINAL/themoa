package com.weaone.themoa.domain.member.repository;

import com.weaone.themoa.domain.member.entity.MemberAdminActionLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MemberAdminActionLogRepository extends JpaRepository<MemberAdminActionLog, Long> {

    /** 특정 회원에 대한 관리자 조치 이력(최신순), 상세 화면 이력 패널용. */
    Page<MemberAdminActionLog> findByTargetMember_IdOrderByCreatedAtDesc(Long targetMemberId, Pageable pageable);
}
