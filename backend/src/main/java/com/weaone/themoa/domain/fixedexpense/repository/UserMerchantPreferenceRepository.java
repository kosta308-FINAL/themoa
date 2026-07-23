package com.weaone.themoa.domain.fixedexpense.repository;

import com.weaone.themoa.domain.fixedexpense.entity.UserMerchantPreference;
import com.weaone.themoa.domain.fixedexpense.entity.UserMerchantPreferenceType;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserMerchantPreferenceRepository extends JpaRepository<UserMerchantPreference, Long> {

    boolean existsByMember_IdAndMerchantAlias_IdAndPreferenceType(
            Long memberId, Long merchantAliasId, UserMerchantPreferenceType preferenceType);

    boolean existsByMember_IdAndBillerMerchant_IdAndPreferenceType(
            Long memberId, Long billerMerchantId, UserMerchantPreferenceType preferenceType);

    boolean existsByMember_IdAndRecurringPaymentGroup_IdAndPreferenceType(
            Long memberId, Long recurringPaymentGroupId, UserMerchantPreferenceType preferenceType);
}
