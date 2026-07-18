package com.weaone.themoa.domain.coaching.repository;

import com.weaone.themoa.domain.coaching.entity.CoachingCard;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface CoachingCardRepository extends JpaRepository<CoachingCard, Long> {

    Optional<CoachingCard> findByIdAndMember_Id(Long id, Long memberId);

    boolean existsByMember_IdAndYearMonth(Long memberId, String yearMonth);

    List<CoachingCard> findByMember_IdAndYearMonthOrderByDisplayOrderAsc(Long memberId, String yearMonth);

    /** 소비 가이드 화면 표시용(§5): 가장 최근에 생성된 주기의 카드 묶음을 보여준다. */
    @Query("select max(c.yearMonth) from CoachingCard c where c.member.id = :memberId")
    Optional<String> findLatestYearMonth(@Param("memberId") Long memberId);
}
