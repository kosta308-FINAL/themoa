package com.weaone.themoa.domain.budget.repository;

import com.weaone.themoa.domain.budget.entity.SurplusFund;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;

public interface SurplusFundRepository extends JpaRepository<SurplusFund, Long> {

    boolean existsByMember_IdAndYearMonth(Long memberId, String yearMonth);

    /** S-01 잉여금 패널 "완료된 주기 합산"(dailyBudget.md §4): 완료 주기 수. surplus_fund는 마감 배치가 종료된 주기에만 적립하므로 전체 카운트가 곧 완료 주기 수다. */
    long countByMember_Id(Long memberId);

    /** 위와 같은 범위의 잉여금 합계. 초과지출한 주기의 음수도 그대로 합산에 반영한다. */
    @Query("select coalesce(sum(s.amount), 0) from SurplusFund s where s.member.id = :memberId")
    BigDecimal sumAmountByMember_Id(@Param("memberId") Long memberId);
}
