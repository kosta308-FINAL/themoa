package com.weaone.themoa.domain.policy.admin.dto.response;

public record AdminRegionCoverageResponse(
        long provinceCount,
        long municipalityCount,
        long cityCount,
        long sgisProvinceExternalCodeCount,
        long sgisMunicipalityExternalCodeCount,
        long sgisExternalCodeCount,
        long legacyRegionCount,
        long standardRegionCount
) {
}
