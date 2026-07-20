package com.weaone.themoa.domain.fixedexpense.repository;

import com.weaone.themoa.domain.fixedexpense.entity.FixedExpenseCandidate;
import com.weaone.themoa.domain.fixedexpense.entity.FixedExpenseCandidateStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface FixedExpenseCandidateRepository extends JpaRepository<FixedExpenseCandidate, Long> {

    Optional<FixedExpenseCandidate> findByRecurringPaymentGroup_Id(Long recurringGroupId);

    /**
     * {@code registerFromCandidate}가 이 후보의 {@code recurringPaymentGroup.merchantAlias/billerMerchant},
     * {@code recommendedCategory}를 그대로 새 FixedExpense에 옮겨 저장하고, 컨트롤러가 세션 밖에서
     * {@code FixedExpenseResponse.from}으로 변환한다 — 초기화 안 된 프록시가 그대로 넘어가지 않도록 미리 fetch join 한다.
     */
    @Query("""
            select c from FixedExpenseCandidate c
            join fetch c.recurringPaymentGroup g
            left join fetch g.merchantAlias
            left join fetch g.billerMerchant
            join fetch c.recommendedCategory
            where c.id = :id and c.member.id = :memberId
            """)
    Optional<FixedExpenseCandidate> findByIdAndMember_Id(Long id, Long memberId);

    /**
     * 소비가이드 요약(dayguide.md §8.2)에서 상위 3건만 보여줄 수 있도록 점수 내림차순으로 정렬한다.
     * 컨트롤러에서 세션 밖에 DTO 변환({@code FixedExpenseCandidateResponse.from})이 이루어지므로
     * recurringPaymentGroup과 그 하위 merchantAlias/billerMerchant를 미리 fetch join 한다.
     */
    @Query("""
            select c from FixedExpenseCandidate c
            join fetch c.recurringPaymentGroup g
            left join fetch g.merchantAlias
            left join fetch g.billerMerchant
            where c.member.id = :memberId and c.status = :status
            order by c.score desc, c.id desc
            """)
    List<FixedExpenseCandidate> findByMember_IdAndStatusOrderByScoreDescIdDesc(Long memberId, FixedExpenseCandidateStatus status);

    List<FixedExpenseCandidate> findByStatus(FixedExpenseCandidateStatus status);
}
