package com.weaone.themoa.domain.policy.admin.dto.request;

public record AdminSearchExplainRequest(
        String query,
        Integer policyId,
        String sourcePolicyId
) {
}
