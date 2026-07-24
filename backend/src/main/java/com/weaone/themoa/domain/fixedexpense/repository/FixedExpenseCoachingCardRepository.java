package com.weaone.themoa.domain.fixedexpense.repository;

import com.weaone.themoa.domain.fixedexpense.entity.FixedExpenseCoachingCard;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface FixedExpenseCoachingCardRepository extends JpaRepository<FixedExpenseCoachingCard, Long> {

    Optional<FixedExpenseCoachingCard> findByIdAndMember_Id(Long id, Long memberId);

    boolean existsByMember_IdAndYearMonth(Long memberId, String yearMonth);

    List<FixedExpenseCoachingCard> findByMember_IdAndYearMonthAndDismissedAtIsNullOrderByDisplayOrderAsc(
            Long memberId, String yearMonth);
}
