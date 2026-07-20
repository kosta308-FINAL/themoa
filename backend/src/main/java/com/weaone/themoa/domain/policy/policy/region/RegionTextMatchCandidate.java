package com.weaone.themoa.domain.policy.policy.region;

import com.weaone.themoa.domain.policy.policy.entity.RegionCode;

record RegionTextMatchCandidate(
        RegionCode region,
        RegionTextMatchType matchType,
        String matchedText,
        int priority
) {
}
