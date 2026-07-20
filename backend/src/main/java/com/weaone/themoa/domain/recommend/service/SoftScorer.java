package com.weaone.themoa.domain.recommend.service;

import com.weaone.themoa.domain.recommend.dto.UserProfile;

import java.util.ArrayList;
import java.util.List;

import com.weaone.themoa.domain.recommend.entity.SavingsProduct;
import com.weaone.themoa.domain.recommend.entity.SavingsType;

/**
 * 2단계 소프트 필터 - 하드필터 통과 상품에 점수를 매긴다.
 * '누구나 받는 점수'를 걷어내고, 사용자별로 실제 갈리는 항목에 가중치를 둔다.
 * (README 점수표를 기반으로 하되, 변별력을 위해 조정)
 */
public final class SoftScorer {

    private SoftScorer() {
    }

    /** 점수 계산 결과. */
    public record ScoreResult(int score, List<String> reasons) {
    }

    public static ScoreResult score(SavingsProduct p, UserProfile u, RateRanking ranking) {
        int total = 0;
        List<String> reasons = new ArrayList<>();

        // 1) 금리 (선호기간 기준 상대순위) — 최대 20
        // ⚠️ 정기예금(DEPOSIT)은 "목돈을 한 번에 예치"하는 상품이라, 사용자가 입력한 월납입가능액을
        // 그대로 적용하면 안 맞다(적금처럼 매달 쌓이는 게 아님). 목돈처럼 쓸 여유가 있다는 신호
        // (hasClearSurplusSlack)가 없으면 정기예금의 금리 이점을 인정하지 않고 낮게 캡을 씌운다.
        int rateScore = ranking.scoreOf(p);
        boolean depositWithoutLumpSumSignal = p.getProductType() == SavingsType.DEPOSIT && !u.hasClearSurplusSlack();
        if (depositWithoutLumpSumSignal) {
            int capped = Math.min(rateScore, 5);
            reasons.add("정기예금은 목돈을 한 번에 넣는 상품이라 월납입 기준으로는 금리 이점이 제한돼요 (+" + capped + ")");
            total += capped;
        } else {
            total += rateScore;
            reasons.add("선호하시는 가입기간 기준으로 금리가 좋은 상품이에요 (+" + rateScore + ")");
        }

        // 2) 위험성향 — 은행 인지도(안정형) vs 실제 금리 순위(공격형)로 구분.
        // ⚠️ 예전엔 "안정형→예금, 공격형→복리"였는데, 목돈 여유 없으면 정기예금 금리를 이미 할인하는
        // 규칙(1번)과 겹쳐서 이 항목이 실질적으로 있으나마나였다(여유 없는 대부분 상황에서 무의미).
        // 사회초년생이 낯선 저축은행에 거부감을 느낀다는 팀 피드백을 반영해, 안정형/중립형은 "얼마나
        // 대중적으로 알려진 은행인지"로, 공격형은 브랜드와 무관하게 "실제 금리가 높은지"로 가른다.
        switch (u.riskType()) {
            case STABLE -> {
                if (KnownBanks.isTop10(p.getCompanyName())) {
                    total += 10;
                    reasons.add("안정형 성향이 선호할 만한 잘 알려진 은행이에요 (+10)");
                }
            }
            case NEUTRAL -> {
                if (KnownBanks.isTop20(p.getCompanyName())) {
                    total += 5;
                    reasons.add("어느 정도 알려진 은행이라 무난하게 선택하기 좋아요 (+5)");
                }
            }
            case AGGRESSIVE -> {
                if (ranking.isTopRate(p, 10)) {
                    total += 10;
                    reasons.add("목표기간 기준 금리가 상위권인 상품이에요 (+10)");
                }
            }
        }

        // 3) 목표기간 — 상품의 '대표(최고금리) 기간'이 목표 개월수와 정확히 얼마나 가까운지
        // (SHORT/MID/LONG 3구간으로 뭉뚱그리면 12개월과 20개월이 같은 구간으로 취급되는 문제가 있어
        //  정확한 개월 차이로 비교한다)
        Integer signatureTerm = RateRanking.signatureTerm(p);
        if (signatureTerm != null) {
            int monthGap = Math.abs(signatureTerm - u.effectiveTargetMonths());
            if (monthGap <= 2) {
                total += 15;
                reasons.add("목표하신 가입기간과 정확히 맞는 상품이에요 (+15)");
            } else if (monthGap <= 6) {
                total += 7;
                reasons.add("목표하신 가입기간과 비슷한 상품이에요 (+7)");
            }
            // 6개월 넘게 차이나면 0점 (목표와 동떨어짐)
        }

        // 4) 우대조건 난이도 — 사용자 감수 여부에 따라 차등
        String difficulty = p.getDifficultyTag();
        if (!u.acceptCondition()) {
            // 우대조건 싫음 → 쉬운 상품 크게 선호, 까다로우면 0
            if ("쉬움".equals(difficulty)) {
                total += 15;
                reasons.add("우대조건이 쉬워서 부담 없이 받을 수 있는 상품이에요 (+15)");
            } else if ("보통".equals(difficulty)) {
                total += 6;
                reasons.add("우대조건 난이도가 보통인 상품이에요 (+6)");
            }
        } else {
            // 우대조건 감수 가능 → 까다로운(대신 고금리) 상품에 가점
            if ("까다로움".equals(difficulty)) {
                total += 8;
                reasons.add("우대조건은 까다롭지만 채우면 금리 혜택이 큰 상품이에요 (+8)");
            } else if ("보통".equals(difficulty)) {
                total += 4;
                reasons.add("우대조건 난이도가 보통인 상품이에요 (+4)");
            } else if ("쉬움".equals(difficulty)) {
                total += 2;
                reasons.add("우대조건이 쉬운 상품이에요 (+2)");
            }
        }

        // 5) 사회초년생 적합 — 젊은 사용자(만 34세 이하)에게만 의미
        if (u.age() <= 34 && Boolean.TRUE.equals(p.getIsYouthFriendly())) {
            total += 10;
            reasons.add("사회초년생에게 특화된 혜택이 있는 상품이에요 (+10)");
        }

        // 6) 비대면 가입 — 편의(소폭)
        if (Boolean.TRUE.equals(p.getIsOnline())) {
            total += 5;
            reasons.add("비대면으로 간편하게 가입할 수 있는 상품이에요 (+5)");
        }

        // 7) 유동성 — 중도에 뺄 수도 있는 사용자는 자유적립식/회전식을 선호
        // ⚠️ "회전식"은 거의 다 정기예금(회전정기예금)이라, 목돈 여유 신호 없이 이 보너스까지 주면
        // 역시 1)번 금리 할인이 무의미해진다 — 회전식 정기예금은 여유 신호가 있을 때만 인정한다.
        if (u.needLiquidity()) {
            if (hasReserveType(p, "F")) {
                total += 8;
                reasons.add("자유적립식이라 원하는 때 자유롭게 납입할 수 있어요 (+8)");
            } else if (p.getProductName() != null && p.getProductName().contains("회전") && !depositWithoutLumpSumSignal) {
                total += 5;
                reasons.add("회전식이라 일정 주기마다 유연하게 해지할 수 있어요 (+5)");
            }
        }

        return new ScoreResult(total, reasons);
    }

    private static boolean hasReserveType(SavingsProduct p, String code) {
        return p.getOptions().stream().anyMatch(o -> code.equals(o.getReserveTypeCode()));
    }
}
