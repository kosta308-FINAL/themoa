package com.weaone.themoa.domain.policy.recommendation.service;

import com.weaone.themoa.common.exception.BusinessException;
import com.weaone.themoa.common.exception.ErrorCode;
import com.weaone.themoa.domain.policy.policy.entity.RegionCode;
import com.weaone.themoa.domain.policy.policy.region.RegionCatalog;
import com.weaone.themoa.domain.policy.policy.region.ResolvedUserRegion;
import com.weaone.themoa.domain.policy.policy.region.SearchRegionLevel;
import com.weaone.themoa.domain.policy.recommendation.dto.response.PolicyRecommendationRegionOptionsResponse;
import com.weaone.themoa.domain.policy.recommendation.dto.response.PolicyRecommendationSidoOptionResponse;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class PolicyRecommendationRegionService {
    private final RegionCatalog regionCatalog;

    public PolicyRecommendationRegionService(RegionCatalog regionCatalog) {
        this.regionCatalog = regionCatalog;
    }

    public ValidatedRegion validate(String residenceSido, String residenceSigungu) {
        String sido = trim(residenceSido);
        String sigungu = trimToNull(residenceSigungu);
        if (!StringUtils.hasText(sido)) {
            throw new BusinessException(ErrorCode.POLICY_RECOMMENDATION_REGION_INVALID);
        }
        if (StringUtils.hasText(sigungu)) {
            RegionCode city = regionCatalog.findCity(sido, sigungu)
                    .orElseThrow(() -> new BusinessException(ErrorCode.POLICY_RECOMMENDATION_REGION_INVALID));
            return new ValidatedRegion(
                    city.getProvince(),
                    city.getCity(),
                    new ResolvedUserRegion(city.getProvince(), city.getCity(), null, SearchRegionLevel.SIGUNGU, city)
            );
        }
        RegionCode province = regionCatalog.findProvince(sido)
                .orElseThrow(() -> new BusinessException(ErrorCode.POLICY_RECOMMENDATION_REGION_INVALID));
        if (hasSigunguOptions(province.getProvince())) {
            throw new BusinessException(ErrorCode.POLICY_RECOMMENDATION_REGION_INVALID);
        }
        return new ValidatedRegion(
                province.getProvince(),
                null,
                new ResolvedUserRegion(province.getProvince(), null, null, SearchRegionLevel.SIDO, province)
        );
    }

    public PolicyRecommendationRegionOptionsResponse options() {
        Map<String, java.util.List<String>> grouped = new LinkedHashMap<>();
        regionCatalog.allRegions().stream()
                .filter(region -> "PROVINCE".equals(region.getRegionLevel()))
                .sorted(Comparator.comparing(RegionCode::getProvince))
                .forEach(region -> grouped.putIfAbsent(region.getProvince(), new java.util.ArrayList<>()));
        regionCatalog.allSpecificRegionsByLongestName().stream()
                .filter(region -> StringUtils.hasText(region.getCity()))
                .sorted(Comparator.comparing(RegionCode::getProvince).thenComparing(RegionCode::getCity))
                .forEach(region -> grouped.computeIfAbsent(region.getProvince(), key -> new java.util.ArrayList<>())
                        .add(region.getCity()));
        List<PolicyRecommendationSidoOptionResponse> items = grouped.entrySet().stream()
                .map(entry -> new PolicyRecommendationSidoOptionResponse(entry.getKey(), entry.getValue().stream().distinct().toList()))
                .toList();
        return new PolicyRecommendationRegionOptionsResponse(items);
    }

    private String trim(String value) {
        return value == null ? "" : value.trim();
    }

    private String trimToNull(String value) {
        String trimmed = trim(value);
        return StringUtils.hasText(trimmed) ? trimmed : null;
    }

    private boolean hasSigunguOptions(String province) {
        return regionCatalog.allSpecificRegionsByLongestName().stream()
                .anyMatch(region -> province.equals(region.getProvince()) && StringUtils.hasText(region.getCity()));
    }

    public record ValidatedRegion(
            String residenceSido,
            String residenceSigungu,
            ResolvedUserRegion resolvedUserRegion
    ) {
    }
}
