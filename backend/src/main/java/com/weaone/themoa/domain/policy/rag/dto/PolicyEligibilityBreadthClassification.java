package com.weaone.themoa.domain.policy.rag.dto;

import java.util.List;

public record PolicyEligibilityBreadthClassification(
        EligibilityBreadth breadth,
        List<String> evidence
) {
    public PolicyEligibilityBreadthClassification {
        breadth = breadth == null ? EligibilityBreadth.UNKNOWN : breadth;
        evidence = evidence == null ? List.of() : List.copyOf(evidence);
    }
}
