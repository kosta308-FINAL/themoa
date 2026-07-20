package com.weaone.themoa.domain.fixedexpense.repository;

import com.weaone.themoa.domain.fixedexpense.entity.FixedExpenseCandidate;
import com.weaone.themoa.domain.fixedexpense.entity.FixedExpenseCandidateStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface FixedExpenseCandidateRepository extends JpaRepository<FixedExpenseCandidate, Long> {

    Optional<FixedExpenseCandidate> findByRecurringPaymentGroup_Id(Long recurringGroupId);

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
