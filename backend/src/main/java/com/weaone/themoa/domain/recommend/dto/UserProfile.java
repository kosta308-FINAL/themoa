package com.weaone.themoa.domain.recommend.dto;

import com.weaone.themoa.domain.recommend.service.PreferredPeriod;
import com.weaone.themoa.domain.recommend.service.RecommendationService;
import com.weaone.themoa.domain.recommend.service.RiskType;

/**
 * 추천 입력 = 회원 기본정보 + 금융성향 진단 결과를 합친 사용자 프로필.
 * (지금은 데모용으로 직접 만들고, 나중에 member/financial_profile에서 채운다.)
 *
 * @param age               나이(세)
 * @param monthlyIncomeManwon 월소득(만원) - 소득 하드필터용. 모르면 null이면 소득필터 건너뜀
 * @param employmentType    취업유형(직장인/프리랜서/무관)
 * @param lowIncome         차상위 여부
 * @param riskType          위험성향
 * @param preferredPeriod   선호 가입기간(목표기간 없을 때만 씀 - 목표기간 있으면 그게 우선)
 * @param monthlyDepositWon 월 납입가능금액(원). 카드연동 소비분석으로 계산된 잉여금이 기본값이고
 *                          사용자가 늘리거나 줄일 수 있는 값 — 추천은 항상 "이 금액 안에서" 이뤄진다.
 *                          목표금액이 이 값을 절대 덮어쓰지 않는다(잉여금 기반 추천이라는 취지를 지키기 위함).
 * @param acceptCondition   우대조건 감수 여부(true=감수 가능)
 * @param needLiquidity     유동성 필요 여부(true=중도에 뺄 수도 있음 → 자유적립식/회전식 선호)
 * @param goalAmountWon     저축목표 금액(원). 모르면 null
 * @param goalMonths        저축목표 기간(개월). 모르면 null. 있으면 preferredPeriod 대신 이걸로 기간매칭.
 *                          (목표기간은 "얼마나 오래 넣을지"를 이미 말해주므로 선호기간과 중복 질문하지 않는다)
 * @param termOverrideMonths 내부용 - "실제 페이스 N개월" 같은 대안 리스트를 만들 때만 쓰는 정확한 개월수 오버라이드.
 *                          웹 입력으로 직접 받지 않고 withExactTargetMonths()로만 설정한다.
 */
public record UserProfile(
        int age,
        Integer monthlyIncomeManwon,
        String employmentType,
        boolean lowIncome,
        RiskType riskType,
        PreferredPeriod preferredPeriod,
        int monthlyDepositWon,
        boolean acceptCondition,
        boolean needLiquidity,
        Integer goalAmountWon,
        Integer goalMonths,
        Integer termOverrideMonths
) {
    /** 실제 기간 매칭에 쓸 선호기간(대략적 구간). 표시용 라벨 등에 사용. */
    public PreferredPeriod effectivePeriod() {
        return PreferredPeriod.fromMonths(effectiveTargetMonths());
    }

    /**
     * 기간 매칭에 쓸 "정확한" 목표 개월수. SHORT/MID/LONG 3구간으로 뭉뚱그리지 않고
     * 정확한 숫자로 비교해야 "12개월 목표"와 "20개월 목표"가 같은 취급을 받는 걸 방지할 수 있다.
     * 우선순위: termOverrideMonths(대안리스트용) > goalMonths(목표기간) > preferredPeriod 대표값(3/15/30).
     */
    public int effectiveTargetMonths() {
        if (termOverrideMonths != null) {
            return termOverrideMonths;
        }
        if (goalMonths != null) {
            return goalMonths;
        }
        // 대표 개월수는 실제 상품 데이터에 존재하는 가입기간(1/3/6/12/24/36개월)과 정확히 겹치는 값으로 잡는다.
        // 예전엔 MID=15, LONG=30처럼 데이터에 없는 애매한 값을 써서, 가장 가까운 실제 기간을 고를 때
        // 두 옵션(예: 24개월 vs 36개월)이 거리 동점이 되어 "장기"를 선택해도 24개월 상품이 튀어나오는
        // 문제가 있었다 — 동점 자체가 안 생기게 실제 구간과 1:1로 맞춘다.
        return switch (preferredPeriod) {
            case SINGLE -> 1;
            case SHORT -> 6;
            case MID -> 24;
            case LONG -> 36;
        };
    }

    /** 남는 돈이 "목돈"으로 인정받으려면 최소 이 금액(원) 이상이어야 한다 (정기예금 최소가입액 통상 수준). */
    private static final long SURPLUS_FLOOR_WON = 500_000;

    /**
     * 정기예금(목돈 예치용)이 말이 되려면 "매달 들어오는 돈"이 아니라 "지금 목돈처럼 쓸 수 있는 여유"가
     * 있어야 한다. 목표금액이 있고 그걸 채우는 데 필요한 돈을 빼고 남는 돈(surplus)이
     * ① 여력의 절반 이상이고 ② 절대금액으로도 SURPLUS_FLOOR_WON(50만원) 이상일 때만 그 남는 부분을
     * 목돈처럼 볼 근거가 있다고 판단한다.
     * ②를 추가한 이유: 목표금액을 아주 작게(예: 10만원) 잡으면 비율 조건은 쉽게 만족되지만, 실제 남는
     * 돈이 몇 십만원 수준이면 정기예금 목돈 취급을 하기엔 여전히 부족하다 — 절대금액 하한이 없으면
     * 이런 극단적인 입력에서 정기예금이 부당하게 상위로 튀어오르는 문제가 있었다.
     */
    public boolean hasClearSurplusSlack() {
        if (goalAmountWon == null || goalMonths == null || goalMonths <= 0) {
            return false;
        }
        long principalOnlyRequired = (long) Math.ceil(goalAmountWon / (double) goalMonths);
        long surplus = monthlyDepositWon - principalOnlyRequired;
        return surplus >= monthlyDepositWon / 2.0 && surplus >= SURPLUS_FLOOR_WON;
    }

    /**
     * ⚠️ 목표 실현가능성 판단은 여기서 안 함 — 원금만 나눈 계산은 이자를 무시해서 부정확하다
     * (예: 월30만원×6개월=180만원 "원금"만 보면 목표 210만원 미달로 보이지만, 실제 적금 이자를 더하면
     * 채워질 수도 있다). 실제 판매중 상품의 진짜 금리로 이자까지 계산하려면 상품 데이터가 필요하므로
     * RecommendationService.assessGoal(profile)을 쓴다(리포지토리 접근이 필요해 여기 UserProfile엔 못 둠).
     */

    /**
     * 목표(금액·기간)는 지우고, 기간 매칭만 정확히 이 개월수로 하는 복사본을 만든다.
     * "실제 페이스 N개월 기준 추천"처럼 다른 기간으로 한 번 더 추천을 돌릴 때 쓴다.
     * (3구간 버킷이 아니라 정확한 개월수로 매칭되도록 termOverrideMonths를 씀)
     */
    public UserProfile withExactTargetMonths(int months) {
        return new UserProfile(age, monthlyIncomeManwon, employmentType, lowIncome, riskType,
                preferredPeriod, monthlyDepositWon, acceptCondition, needLiquidity, null, null, months);
    }

    /**
     * 월 납입액만 바꾼 복사본(목표는 지움, termOverrideMonths는 유지).
     * "여력 전액 기준"과 "목표에 필요한 최소 금액 기준"을 각각 별도의 추천 목록으로 뽑을 때 쓴다.
     * ⚠️ 하드필터의 max_amount(월납입한도) 체크가 monthlyDepositWon 하나로만 이뤄지기 때문에,
     * 여력(예: 200만원) 그대로 돌리면 월한도가 낮은 상품(예: 30만원)이 실제로는 목표에 충분한데도
     * 부당하게 제외된다 — 그래서 "목표 최소금액" 기준 추천은 반드시 이 메서드로 낮춘 금액을 써서 별도로 돌려야 한다.
     */
    public UserProfile withMonthlyDeposit(int monthlyWon) {
        return new UserProfile(age, monthlyIncomeManwon, employmentType, lowIncome, riskType,
                preferredPeriod, monthlyWon, acceptCondition, needLiquidity, null, null, termOverrideMonths);
    }
}
