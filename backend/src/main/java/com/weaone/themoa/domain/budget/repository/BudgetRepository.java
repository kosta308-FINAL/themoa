package com.weaone.themoa.domain.budget.repository;

import com.weaone.themoa.domain.budget.entity.Budget;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface BudgetRepository extends JpaRepository<Budget, Long> {

    Optional<Budget> findByMember_IdAndYearMonth(Long memberId, String yearMonth);

    Optional<Budget> findByIdAndMember_Id(Long id, Long memberId);

    /** 주기 마감 배치 대상: 종료일이 지난(오늘 이전) 완료 주기(dailyBudget.md §4, MOA-S-BUD-BGT-11). */
    List<Budget> findByCycleEndDateBefore(LocalDate date);

    /** 카테고리 도넛 주기 이동(§3.4): 실제 생성된 주기 중 바로 이전/다음 주기를 찾는다(생성된 적 없는 주기는 건너뛴다). */
    Optional<Budget> findFirstByMember_IdAndCycleStartDateLessThanOrderByCycleStartDateDesc(Long memberId, LocalDate cycleStartDate);

    Optional<Budget> findFirstByMember_IdAndCycleStartDateGreaterThanOrderByCycleStartDateAsc(Long memberId, LocalDate cycleStartDate);

    /**
     * 전체 소비내역 상세 주기 이동(consumeHistoryDetail.md §4.1): 도넛과 달리 "가장 가까운 저장된 주기"가
     * 아니라 "바로 연속된 주기"만 허용한다. 후보를 찾은 뒤 호출자가 날짜 연속성(끝일+1=시작일)을 검증해야
     * 한다 — 중간 주기가 비어 있으면 더 오래된 주기를 직전 주기로 잘못 노출하지 않기 위해서다.
     */
    Optional<Budget> findFirstByMember_IdAndCycleEndDateBeforeOrderByCycleEndDateDesc(
            Long memberId, LocalDate cycleStartDate);

    Optional<Budget> findFirstByMember_IdAndCycleStartDateAfterOrderByCycleStartDateAsc(
            Long memberId, LocalDate cycleEndDate);

    /**
     * 카테고리 소비 상세 최근 추이(categoryDetail.md §6.5): 선택 주기를 포함해 최근 최대 4개 주기 후보를
     * 최신순으로 가져온다. 호출자가 앞에서부터 날짜 연속성(끝일+1=다음 시작일)을 검증해 끊긴 지점에서 자른다.
     */
    List<Budget> findTop4ByMember_IdAndCycleStartDateLessThanEqualOrderByCycleStartDateDesc(
            Long memberId, LocalDate cycleStartDate);
}
