package com.weaone.themoa.domain.subscription.repository;

import com.weaone.themoa.domain.subscription.entity.SavingsSubscriptionCondition;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SavingsSubscriptionConditionRepository extends JpaRepository<SavingsSubscriptionCondition, Long> {

    /** 본인 가입 건의 조건만 토글할 수 있도록 소유권까지 확인해 조회한다. */
    Optional<SavingsSubscriptionCondition> findByIdAndSubscription_Member_Id(Long id, Long memberId);
}
