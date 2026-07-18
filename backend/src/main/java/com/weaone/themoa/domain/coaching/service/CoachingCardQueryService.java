package com.weaone.themoa.domain.coaching.service;

import com.weaone.themoa.domain.coaching.entity.CoachingCard;
import com.weaone.themoa.domain.coaching.repository.CoachingCardRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 소비 가이드 화면 코칭 카드 조회(habitExpense.md §5). 가장 최근에 생성된 주기의 카드 묶음을 보여준다 —
 * 카드 미연동이거나 후보 하한을 통과한 항목이 없으면 정상 200 빈 배열이다(오류 아님).
 */
@Service
@RequiredArgsConstructor
public class CoachingCardQueryService {

    private final CoachingCardRepository coachingCardRepository;

    @Transactional(readOnly = true)
    public List<CoachingCard> listLatest(Long memberId) {
        return coachingCardRepository.findLatestYearMonth(memberId)
                .map(yearMonth -> coachingCardRepository.findByMember_IdAndYearMonthOrderByDisplayOrderAsc(memberId, yearMonth))
                .orElseGet(List::of);
    }
}
