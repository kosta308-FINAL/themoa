package com.weaone.themoa.domain.policy.policy.region;

import com.weaone.themoa.domain.policy.policy.entity.RegionCode;

import java.util.Set;

public record InstitutionRegionResult(
        InstitutionRegionType type,
        Set<RegionCode> regions
) {
}
