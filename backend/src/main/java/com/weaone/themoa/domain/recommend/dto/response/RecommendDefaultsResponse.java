package com.weaone.themoa.domain.recommend.dto.response;

import com.weaone.themoa.domain.recommend.service.PreferredPeriod;
import com.weaone.themoa.domain.recommend.service.RiskType;

/**
 * 추천 입력 폼의 기본값. 계산 가능한 값과 회원이 마지막으로 사용한 추천 조건을 미리 채워준다.
 *
 * @param age                  회원가입 시 받은 생년월일로 계산한 만 나이. 화면에서 직접 수정할 수 있지만
 *                             초기값은 실제 나이여야 한다(고정값으로 두면 사용자가 안 고쳤을 때 존재하지도
 *                             않는 나이 기준으로 추천되어, 나이 제한 상품이 잘못 추천/제외될 수 있다).
 * @param monthlyIncomeManwon 월소득(만원). 소비가이드 설정 전이라 알 수 없으면 null → 화면에서 직접 입력받는다.
 * @param monthlyDepositWon   월 납입가능금액(원). 완료 주기 잉여금에서 이번 주기들로 가져간 금액을 뺀
 *                            순잔액의 주기당 평균이며, 적립 이력이 없으면 기본값을 내려준다.
 * @param depositFromSurplus  monthlyDepositWon이 잉여금 순잔액에서 온 값이면 true, 기본값이면 false.
 *                            화면에서 "지난 주기 잉여금 기준"인지 안내할 때 쓴다.
 * @param employmentType      마지막 추천의 취업유형. 저장 이력이 없으면 null.
 * @param lowIncome           마지막 추천의 차상위계층 여부.
 * @param riskType            마지막 추천의 위험성향. 저장 이력이 없으면 null.
 * @param preferredPeriod     마지막 추천의 선호 가입기간. 저장 이력이 없으면 null.
 * @param acceptCondition     마지막 추천의 우대조건 감수 여부.
 * @param needLiquidity       마지막 추천의 유동성 중요 여부.
 * @param goalAmountWon       마지막 추천의 저축목표 금액. 목표 없음이면 null.
 * @param goalMonths          마지막 추천의 저축목표 기간. 목표 없음이면 null.
 */
public record RecommendDefaultsResponse(
        int age,
        Integer monthlyIncomeManwon,
        int monthlyDepositWon,
        boolean depositFromSurplus,
        String employmentType,
        boolean lowIncome,
        RiskType riskType,
        PreferredPeriod preferredPeriod,
        boolean acceptCondition,
        boolean needLiquidity,
        Integer goalAmountWon,
        Integer goalMonths
) {
}
