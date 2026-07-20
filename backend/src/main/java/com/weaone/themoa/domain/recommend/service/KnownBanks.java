package com.weaone.themoa.domain.recommend.service;

import java.util.List;

/**
 * 은행 인지도 순위(대중성 기준) — 안정형/중립형 위험성향 점수에 쓴다.
 * DB에 실제 저장된 company_name 표기와 정확히 일치해야 매칭된다(회사명 앞뒤 "주식회사" 등 포함).
 * 순위는 시중은행·인터넷은행 우선, 그다음 지방은행/대형 저축은행 순으로 사람이 직접 선정한 목록이라
 * 절대적 기준은 아니다(팀 협의로 언제든 조정 가능).
 */
final class KnownBanks {

    private KnownBanks() {
    }

    /**
     * 상위 10위 — 안정형이 선호할 만큼 대중적으로 익숙한 은행.
     * 부산은행/아이엠뱅크(구 대구은행)는 지방은행이라 전국 인지도는 시중은행급이 아니라고 판단해서
     * 빼고, TV광고 등으로 저축은행 중 전국적으로 잘 알려진 OK저축은행/SBI저축은행으로 교체함.
     */
    private static final List<String> TOP_10 = List.of(
            "국민은행",
            "신한은행",
            "우리은행",
            "주식회사 하나은행",
            "농협은행주식회사",
            "중소기업은행",
            "주식회사 카카오뱅크",
            "주식회사 케이뱅크",
            "OK저축은행",
            "SBI저축은행"
    );

    /** 11~20위 — 중립형까지 포함해서 "그럭저럭 알려진" 은행/대형 저축은행. TOP_10을 포함한 전체 20개. */
    private static final List<String> TOP_20_EXTRA = List.of(
            "부산은행",
            "아이엠뱅크",
            "광주은행",
            "전북은행",
            "제주은행",
            "한국산업은행",
            "웰컴저축은행",
            "페퍼저축은행",
            "다올저축은행",
            "상상인저축은행"
    );

    static boolean isTop10(String companyName) {
        return TOP_10.contains(companyName);
    }

    static boolean isTop20(String companyName) {
        return TOP_10.contains(companyName) || TOP_20_EXTRA.contains(companyName);
    }
}
