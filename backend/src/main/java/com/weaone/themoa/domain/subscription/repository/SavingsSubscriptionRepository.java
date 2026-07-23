package com.weaone.themoa.domain.subscription.repository;

import com.weaone.themoa.domain.subscription.entity.SavingsSubscription;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface SavingsSubscriptionRepository extends JpaRepository<SavingsSubscription, Long> {

    /** 대시보드 목록: 조건까지 함께 로딩(N+1 방지), 최근 가입 순. */
    @Query("""
            select distinct s from SavingsSubscription s
            left join fetch s.conditions
            where s.member.id = :memberId
            order by s.createdAt desc
            """)
    List<SavingsSubscription> findAllWithConditionsByMemberId(@Param("memberId") Long memberId);

    Optional<SavingsSubscription> findByIdAndMember_Id(Long id, Long memberId);
}
