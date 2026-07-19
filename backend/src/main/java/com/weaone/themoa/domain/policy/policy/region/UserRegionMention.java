package com.weaone.themoa.domain.policy.policy.region;

public record UserRegionMention(
        UserRegionRole role,
        UserRegionResolution resolution,
        String evidence,
        int startIndex,
        int endIndex
) {
}
