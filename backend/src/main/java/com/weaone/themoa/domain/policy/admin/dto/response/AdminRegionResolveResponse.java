package com.weaone.themoa.domain.policy.admin.dto.response;

import com.weaone.themoa.domain.policy.policy.region.RegionCandidate;
import com.weaone.themoa.domain.policy.policy.region.RegionTextMatchType;
import com.weaone.themoa.domain.policy.policy.region.SearchRegionLevel;
import com.weaone.themoa.domain.policy.policy.region.UserRegionResolutionStatus;

import java.util.List;

public record AdminRegionResolveResponse(
        String query,
        UserRegionResolutionStatus status,
        SearchRegionLevel regionLevel,
        String province,
        String city,
        String displayName,
        String matchedText,
        RegionTextMatchType matchType,
        Integer regionId,
        String internalCode,
        String regionName,
        List<AdminExternalCodeResponse> externalCodes,
        List<RegionCandidate> candidates
) {
}
