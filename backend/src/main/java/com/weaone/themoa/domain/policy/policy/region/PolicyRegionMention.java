package com.weaone.themoa.domain.policy.policy.region;

import com.weaone.themoa.domain.policy.policy.domain.RegionCode;

public record PolicyRegionMention(
        RegionCode region,
        PolicyRegionMentionRole role,
        String rawText,
        int confidence,
        String reason
) {
}
