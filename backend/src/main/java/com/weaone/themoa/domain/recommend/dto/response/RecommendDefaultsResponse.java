package com.weaone.themoa.domain.recommend.dto.response;

/**
 * 추천 입력 폼의 기본값. 회원가입·소비내역 연동으로 이미 알고 있는 값을 미리 채워주기 위한 응답이다.
 *
 * @param age                  회원가입 시 받은 생년월일로 계산한 만 나이. 화면에서 직접 수정할 수 있지만
 *                             초기값은 실제 나이여야 한다(고정값으로 두면 사용자가 안 고쳤을 때 존재하지도
 *                             않는 나이 기준으로 추천되어, 나이 제한 상품이 잘못 추천/제외될 수 있다).
 * @param monthlyIncomeManwon 월소득(만원). 소비가이드 설정 전이라 알 수 없으면 null → 화면에서 직접 입력받는다.
 * @param monthlyDepositWon   월 납입가능금액(원). 최근 급여주기 잉여금을 쓰고, 아직 적립된 잉여금이 없으면
 *                            기본값을 내려준다(항상 값이 있다).
 * @param depositFromSurplus  monthlyDepositWon이 실제 잉여금 기록에서 온 값이면 true, 기본값이면 false.
 *                            화면에서 "지난 주기 잉여금 기준"인지 안내할 때 쓴다.
 */
public record RecommendDefaultsResponse(
        int age,
        Integer monthlyIncomeManwon,
        int monthlyDepositWon,
        boolean depositFromSurplus
) {
}
