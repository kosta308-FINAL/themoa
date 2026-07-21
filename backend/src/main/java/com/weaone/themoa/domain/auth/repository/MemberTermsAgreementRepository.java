package com.weaone.themoa.domain.auth.repository;

import com.weaone.themoa.domain.auth.entity.MemberTermsAgreement;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MemberTermsAgreementRepository extends JpaRepository<MemberTermsAgreement, Long> {
}
