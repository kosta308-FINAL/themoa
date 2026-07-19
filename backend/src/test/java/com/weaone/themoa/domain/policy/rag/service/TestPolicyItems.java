package com.weaone.themoa.domain.policy.rag.service;

import com.weaone.themoa.domain.policy.rag.dto.PolicySearchResultItem;

import java.util.List;

public final class TestPolicyItems {
    private TestPolicyItems() {
    }

    public static PolicySearchResultItem item(String tier) {
        return item(1, tier, "NATIONWIDE", 80.0);
    }

    public static PolicySearchResultItem item(Integer id, String tier, String regionCompatibility, double finalScore) {
        return new PolicySearchResultItem(id, "SRC-" + id, "정책 " + id, "복지", "전국",
                "MATCHED", "지역 일치", List.of(), "기관", "요약", 19, 39, null,
                null, null, "OPEN", null, 0.7, finalScore, List.of(), List.of(), regionCompatibility,
                regionCompatibility, "지역 일치", 100, List.of(), "MATCHED", "나이 일치",
                "UNKNOWN", "취업 확인", "UNKNOWN", "학생 확인", 0.5, "GENERAL", List.of(),
                List.of(), List.of(), true, List.of(), List.of(), List.of(), "UNKNOWN", "교육 확인",
                List.of(), false, List.of(), tier, tier);
    }
}
