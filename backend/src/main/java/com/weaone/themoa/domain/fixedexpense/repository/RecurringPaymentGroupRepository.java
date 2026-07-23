package com.weaone.themoa.domain.fixedexpense.repository;

import com.weaone.themoa.domain.fixedexpense.entity.RecurringPaymentGroup;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RecurringPaymentGroupRepository extends JpaRepository<RecurringPaymentGroup, Long> {

    /** UNIQUE 없음(§ erd.md) — 같은 alias 아래 별개 구독이 공존할 수 있어 증거 거래 겹침으로 find-or-create한다. */
    List<RecurringPaymentGroup> findByMember_IdAndMerchantAlias_Id(Long memberId, Long merchantAliasId);

    /** biller형도 동일한 이유로 UNIQUE가 없어 금액 유사도로 기존 그룹을 찾는 find-or-create가 필요하다. */
    List<RecurringPaymentGroup> findByMember_IdAndBillerMerchant_Id(Long memberId, Long billerMerchantId);
}
