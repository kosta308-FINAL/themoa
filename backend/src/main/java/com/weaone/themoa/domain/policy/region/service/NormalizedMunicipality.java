package com.weaone.themoa.domain.policy.region.service;

public record NormalizedMunicipality(
        String provinceName,
        String municipalityName,
        String sourceCode,
        boolean supported,
        String ignoredReason
) {
}
