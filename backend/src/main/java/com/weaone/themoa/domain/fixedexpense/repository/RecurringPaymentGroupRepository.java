package com.weaone.themoa.domain.fixedexpense.repository;

import com.weaone.themoa.domain.fixedexpense.entity.RecurringPaymentGroup;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RecurringPaymentGroupRepository extends JpaRepository<RecurringPaymentGroup, Long> {

    Optional<RecurringPaymentGroup> findByMember_IdAndMerchantAlias_Id(Long memberId, Long merchantAliasId);

    /** biller형은 UNIQUE가 없어(§ erd.md) 금액 유사도로 기존 그룹을 찾는 find-or-create가 필요하다. */
    List<RecurringPaymentGroup> findByMember_IdAndBillerMerchant_Id(Long memberId, Long billerMerchantId);
}
