package com.weaone.themoa.domain.policy.rag.dto;

import java.util.List;
import java.util.Set;

public record UserOfferingIntent(
        Set<PolicyOfferingType> preferredTypes,
        boolean broadBenefitIntent,
        boolean explicitProgramIntent,
        List<String> evidence
) {
    public UserOfferingIntent {
        preferredTypes = preferredTypes == null ? Set.of() : Set.copyOf(preferredTypes);
        evidence = evidence == null ? List.of() : List.copyOf(evidence);
    }
}
