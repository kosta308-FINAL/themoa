package com.weaone.themoa.domain.fixedexpense.repository;

import com.weaone.themoa.domain.fixedexpense.entity.UserMerchantPreference;
import com.weaone.themoa.domain.fixedexpense.entity.UserMerchantPreferenceType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface UserMerchantPreferenceRepository extends JpaRepository<UserMerchantPreference, Long> {

    boolean existsByMember_IdAndMerchantAlias_IdAndPreferenceType(
            Long memberId, Long merchantAliasId, UserMerchantPreferenceType preferenceType);

    boolean existsByMember_IdAndBillerMerchant_IdAndPreferenceType(
            Long memberId, Long billerMerchantId, UserMerchantPreferenceType preferenceType);

    /** 관리자 서비스 병합 시 정리 대상(회원별 UNIQUE라 옮기지 않고 지운다). */
    List<UserMerchantPreference> findByMerchantAlias_Id(Long merchantAliasId);
}
