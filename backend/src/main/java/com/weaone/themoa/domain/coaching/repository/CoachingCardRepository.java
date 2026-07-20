package com.weaone.themoa.domain.coaching.repository;

import com.weaone.themoa.domain.coaching.entity.CoachingCard;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CoachingCardRepository extends JpaRepository<CoachingCard, Long> {

    Optional<CoachingCard> findByIdAndMember_Id(Long id, Long memberId);

    boolean existsByMember_IdAndYearMonth(Long memberId, String yearMonth);

    List<CoachingCard> findByMember_IdAndYearMonthAndDismissedAtIsNullOrderByDisplayOrderAsc(Long memberId,
                                                                                              String yearMonth);
}
