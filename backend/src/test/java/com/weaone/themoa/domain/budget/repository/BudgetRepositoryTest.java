package com.weaone.themoa.domain.budget.repository;

import com.weaone.themoa.domain.budget.entity.Budget;
import com.weaone.themoa.domain.member.entity.Gender;
import com.weaone.themoa.domain.member.entity.Member;
import com.weaone.themoa.domain.member.repository.MemberRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/** 전체 소비내역 상세 주기 이동 후보 조회(consumeHistoryDetail.md §4.1·§7.1) 검증. 테스트 DB는 실제 MySQL이다. */
@SpringBootTest
@Transactional
class BudgetRepositoryTest {

    @Autowired
    private MemberRepository memberRepository;
    @Autowired
    private BudgetRepository budgetRepository;

    private Member persistMember(String email) {
        return memberRepository.save(
                Member.signUp(email, "hash", "닉네임", Gender.MALE, LocalDate.of(2000, 1, 1), LocalDateTime.now()));
    }

    private Budget persistBudget(Member member, String yearMonth, LocalDate start, LocalDate end) {
        return budgetRepository.save(Budget.openCycle(member, yearMonth, start, end,
                BigDecimal.valueOf(2_000_000), BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO));
    }

    @Test
    @DisplayName("직전 주기 후보는 끝일이 선택 주기 시작일보다 이른 것 중 가장 가까운 것을 찾는다")
    void findsNearestPreviousCandidateByEndDate() {
        Member member = persistMember("budget-prev@example.com");
        persistBudget(member, "2026-05", LocalDate.of(2026, 5, 5), LocalDate.of(2026, 6, 4));
        Budget selected = persistBudget(member, "2026-07", LocalDate.of(2026, 7, 5), LocalDate.of(2026, 8, 4));

        Optional<Budget> candidate = budgetRepository
                .findFirstByMember_IdAndCycleEndDateBeforeOrderByCycleEndDateDesc(member.getId(), selected.getCycleStartDate());

        // 6월 주기가 없어 중간이 비어 있으므로, 후보 자체는 나오지만(6/4) 연속성 검증은 서비스 계층 책임이다.
        assertThat(candidate).isPresent();
        assertThat(candidate.get().getYearMonth()).isEqualTo("2026-05");
        assertThat(candidate.get().getCycleEndDate().plusDays(1)).isNotEqualTo(selected.getCycleStartDate());
    }

    @Test
    @DisplayName("연속된 직전·다음 주기는 끝일+1=다음 시작일 관계로 정확히 찾힌다")
    void findsAdjacentPreviousAndNext() {
        Member member = persistMember("budget-adjacent@example.com");
        Budget previous = persistBudget(member, "2026-06", LocalDate.of(2026, 6, 5), LocalDate.of(2026, 7, 4));
        Budget selected = persistBudget(member, "2026-07", LocalDate.of(2026, 7, 5), LocalDate.of(2026, 8, 4));
        Budget next = persistBudget(member, "2026-08", LocalDate.of(2026, 8, 5), LocalDate.of(2026, 9, 4));

        Optional<Budget> previousCandidate = budgetRepository
                .findFirstByMember_IdAndCycleEndDateBeforeOrderByCycleEndDateDesc(member.getId(), selected.getCycleStartDate());
        Optional<Budget> nextCandidate = budgetRepository
                .findFirstByMember_IdAndCycleStartDateAfterOrderByCycleStartDateAsc(member.getId(), selected.getCycleEndDate());

        assertThat(previousCandidate).contains(previous);
        assertThat(nextCandidate).contains(next);
    }

    @Test
    @DisplayName("선택 주기가 가장 오래된 주기면 직전 후보가 없다")
    void noPreviousCandidateAtEarliestCycle() {
        Member member = persistMember("budget-none@example.com");
        Budget only = persistBudget(member, "2026-07", LocalDate.of(2026, 7, 5), LocalDate.of(2026, 8, 4));

        Optional<Budget> candidate = budgetRepository
                .findFirstByMember_IdAndCycleEndDateBeforeOrderByCycleEndDateDesc(member.getId(), only.getCycleStartDate());

        assertThat(candidate).isEmpty();
    }
}
