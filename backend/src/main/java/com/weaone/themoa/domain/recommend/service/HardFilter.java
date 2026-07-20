package com.weaone.themoa.domain.recommend.service;

import com.weaone.themoa.domain.recommend.dto.UserProfile;

import com.weaone.themoa.domain.recommend.entity.SavingsProduct;
import com.weaone.themoa.domain.recommend.entity.SavingsType;

/**
 * 1단계 하드 필터 - 가입 자격이 안 되는 상품을 점수계산 전에 완전히 제외한다.
 * (README의 WHERE 조건과 동일한 규칙)
 */
public final class HardFilter {

    private HardFilter() {
    }

    /** 사용자가 가입 가능한 상품이면 true. */
    public static boolean passes(SavingsProduct p, UserProfile u) {
        if (isClosed(p.getCloseDate())) {
            return false;   // 판매종료
        }
        if (p.getMinAge() != null && p.getMinAge() > u.age()) {
            return false;   // 최소나이 미달
        }
        if (p.getMaxAge() != null && p.getMaxAge() < u.age()) {
            return false;   // 최대나이 초과
        }
        if (u.monthlyIncomeManwon() != null) {
            if (p.getIncomeLimit() != null && p.getIncomeLimit() < u.monthlyIncomeManwon()) {
                return false;   // 소득 상한 초과
            }
            if (p.getIncomeMin() != null && p.getIncomeMin() > u.monthlyIncomeManwon()) {
                return false;   // 소득 하한 미달
            }
        }
        if (p.getEmploymentType() != null && !"무관".equals(p.getEmploymentType())
                && !p.getEmploymentType().equals(u.employmentType())) {
            return false;   // 취업유형 불일치
        }
        if (Boolean.TRUE.equals(p.getIsForLowIncome()) && !u.lowIncome()) {
            return false;   // 서민전용인데 사용자가 차상위 아님
        }
        // 적금은 max_amount가 월납입한도. 실제로 사용자가 넣을 수 있는 금액(월납입가능액 - 카드연동
        // 잉여금 기반, 목표금액이 아니라 이게 진짜 기준)이 한도를 넘으면 제외한다.
        // 목표금액이 크다고 이 기준을 목표 필요액으로 바꾸지 않는다 — "가진 돈 안에서 추천"이 원칙.
        if (p.getProductType() == SavingsType.SAVING && p.getMaxAmount() != null
                && u.monthlyDepositWon() > p.getMaxAmount()) {
            return false;
        }
        return true;
    }

    private static boolean isClosed(String dclsEndDay) {
        return dclsEndDay != null && !dclsEndDay.isBlank();
    }
}
