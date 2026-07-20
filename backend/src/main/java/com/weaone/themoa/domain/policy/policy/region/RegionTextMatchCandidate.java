package com.weaone.themoa.domain.policy.policy.region;

import com.weaone.themoa.domain.policy.policy.domain.RegionCode;

record RegionTextMatchCandidate(
        RegionCode region,
        RegionTextMatchType matchType,
        String matchedText,
        int priority
) {
}
