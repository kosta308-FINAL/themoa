package com.weaone.themoa.domain.auth.repository;

import com.weaone.themoa.domain.auth.entity.MemberTermsAgreement;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MemberTermsAgreementRepository extends JpaRepository<MemberTermsAgreement, Long> {

    /** 마이페이지 약관 동의 이력(erd.md §1) — 최신 동의가 먼저 보이도록 정렬. */
    List<MemberTermsAgreement> findByMember_IdOrderByAgreedAtDesc(Long memberId);
}
