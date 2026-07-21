package com.weaone.themoa.domain.recommend.repository;

import com.weaone.themoa.domain.budget.entity.Budget;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * 추천 폼 기본값(월소득)에 쓸 급여주기 스냅샷을 읽는 전용 레포지토리.
 *
 * <p>{@code budget.salary_amount}는 해당 주기의 소득 스냅샷이라, 고정월급(SALARY)이든 시급제(HOURLY)든
 * 이미 계산된 값이 들어있다. 그래서 소득유형별 계산을 다시 구현하지 않고 이 값을 그대로 쓴다.
 */
public interface RecommendBudgetRepository extends JpaRepository<Budget, Long> {

    Optional<Budget> findFirstByMember_IdOrderByCycleStartDateDesc(Long memberId);
}
