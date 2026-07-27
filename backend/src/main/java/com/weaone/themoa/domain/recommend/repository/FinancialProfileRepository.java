package com.weaone.themoa.domain.recommend.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.weaone.themoa.domain.recommend.entity.FinancialProfile;

public interface FinancialProfileRepository extends JpaRepository<FinancialProfile, Long> {

    Optional<FinancialProfile> findByMember_Id(Long memberId);
}
