package com.weaone.themoa.domain.policy.policy.region;

import com.weaone.themoa.domain.policy.policy.entity.RegionCode;

public enum SearchRegionLevel {
    NATIONWIDE,
    SIDO,
    SIGUNGU;

    public static SearchRegionLevel from(RegionCode region) {
        if (region == null) {
            return null;
        }
        if ("NATIONWIDE".equals(region.getRegionLevel()) || "KR".equals(region.getRegionCode())) {
            return NATIONWIDE;
        }
        if ("PROVINCE".equals(region.getRegionLevel())) {
            return SIDO;
        }
        if ("CITY".equals(region.getRegionLevel()) || "DISTRICT".equals(region.getRegionLevel())) {
            return SIGUNGU;
        }
        return null;
    }
}
