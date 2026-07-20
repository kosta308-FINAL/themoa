package com.weaone.themoa.domain.policy.policy.service;

public record PolicyUpsertResult(
        int policyId,
        boolean inserted
) {
}
