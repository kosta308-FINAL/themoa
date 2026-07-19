package com.weaone.themoa.domain.policy.rag.dto;

import java.util.List;

public record PolicyOfferingTypeClassification(
        PolicyOfferingType type,
        List<String> evidence
) {
    public PolicyOfferingTypeClassification {
        type = type == null ? PolicyOfferingType.UNKNOWN : type;
        evidence = evidence == null ? List.of() : List.copyOf(evidence);
    }

    public static PolicyOfferingTypeClassification unknown() {
        return new PolicyOfferingTypeClassification(PolicyOfferingType.UNKNOWN, List.of("정책 제공 형태 근거 없음"));
    }
}
