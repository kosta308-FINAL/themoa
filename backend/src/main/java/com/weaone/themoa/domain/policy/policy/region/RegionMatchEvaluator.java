package com.weaone.themoa.domain.policy.policy.region;

import com.weaone.themoa.domain.policy.policy.entity.Policy;
import com.weaone.themoa.domain.policy.policy.entity.RegionCode;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Objects;
import java.util.Set;

@Component
public class RegionMatchEvaluator {
    private final RegionCatalog catalog;
    private final RegionNormalizer normalizer;

    public RegionMatchEvaluator(RegionCatalog catalog, RegionNormalizer normalizer) {
        this.catalog = catalog;
        this.normalizer = normalizer;
    }

    public ResolvedUserRegion resolveUserRegion(String province, String city, String district) {
        return resolveUserRegion(province, city, district, null);
    }

    public ResolvedUserRegion resolveUserRegion(String province, String city, String district, String regionLevel) {
        String normalizedProvince = normalizer.normalizeProvince(province);
        String normalizedCity = normalizer.normalizeCity(city);
        if (SearchRegionLevel.NATIONWIDE.name().equals(regionLevel) || "전국".equals(normalizedProvince)) {
            return new ResolvedUserRegion("전국", null, null, SearchRegionLevel.NATIONWIDE,
                    catalog.nationwide().orElse(null));
        }
        if (!StringUtils.hasText(normalizedProvince) && StringUtils.hasText(normalizedCity)) {
            for (RegionCode region : catalog.allSpecificRegionsByLongestName()) {
                if (normalizedCity.equals(region.getCity()) || region.getCity() != null && region.getCity().contains(normalizedCity)) {
                    normalizedProvince = region.getProvince();
                    normalizedCity = cityPart(region);
                    break;
                }
            }
        }
        if (StringUtils.hasText(district) && !StringUtils.hasText(normalizedCity)) {
            for (RegionCode region : catalog.allSpecificRegionsByLongestName()) {
                if (region.getCity() != null && region.getCity().contains(district)) {
                    normalizedProvince = region.getProvince();
                    normalizedCity = cityPart(region);
                    break;
                }
            }
        }
        RegionCode resolvedRegion = catalog.findProvinceOrCity(normalizedProvince, normalizedCity).orElse(null);
        SearchRegionLevel resolvedLevel = resolvedRegion == null
                ? new ResolvedUserRegion(emptyToNull(normalizedProvince), emptyToNull(normalizedCity), emptyToNull(district)).level()
                : SearchRegionLevel.from(resolvedRegion);
        return new ResolvedUserRegion(emptyToNull(normalizedProvince), emptyToNull(normalizedCity), emptyToNull(district),
                resolvedLevel, resolvedRegion);
    }

    public RegionMatchResult evaluate(Policy policy, ResolvedUserRegion userRegion) {
        if (userRegion == null || !userRegion.hasRegion()) {
            return result(RegionCompatibility.UNKNOWN, true, 0, "지역 조건 없음");
        }
        Set<com.weaone.themoa.domain.policy.policy.entity.PolicyRegion> regions = policy.getRegions();
        if (regions.isEmpty()) {
            return result(RegionCompatibility.REGION_UNSPECIFIED, true, 0, "지역 제한이 명시되지 않은 정책입니다.");
        }
        List<RegionCode> policyRegions = regions.stream()
                .map(com.weaone.themoa.domain.policy.policy.entity.PolicyRegion::getRegion)
                .filter(Objects::nonNull)
                .toList();
        if (policyRegions.isEmpty()) {
            return result(RegionCompatibility.UNKNOWN, false, 0, "정책 적용 지역을 확인할 수 없습니다.");
        }
        if (userRegion.nationwide()) {
            if (policyRegions.stream().anyMatch(this::isNationwide)) {
                return result(RegionCompatibility.NATIONWIDE, true, 100, "전국에서 신청 가능한 정책");
            }
            return result(RegionCompatibility.NOT_MATCHED, false, 0, "전국 전용 정책이 아닙니다.");
        }

        boolean multiple = policyRegions.size() > 1;
        RegionCode userRegionCode = userRegion.region();
        SearchRegionLevel userLevel = userRegion.level();
        RegionCode userSido = userLevel == SearchRegionLevel.SIGUNGU ? parentSido(userRegionCode, userRegion) : userRegionCode;
        boolean exactSigunguMatch = false;
        boolean sidoMatch = false;
        boolean childSigunguMatch = false;
        boolean nationwide = false;

        for (RegionCode region : policyRegions) {
            SearchRegionLevel policyLevel = SearchRegionLevel.from(region);
            if (isNationwide(region)) {
                nationwide = true;
                continue;
            }
            if (userLevel == SearchRegionLevel.SIGUNGU) {
                if (sameSigungu(region, userRegionCode, userRegion)) {
                    exactSigunguMatch = true;
                    continue;
                }
                if (policyLevel == SearchRegionLevel.SIDO && sameSido(region, userSido, userRegion.province())) {
                    sidoMatch = true;
                }
            } else if (userLevel == SearchRegionLevel.SIDO) {
                if (policyLevel == SearchRegionLevel.SIDO && sameSido(region, userRegionCode, userRegion.province())) {
                    sidoMatch = true;
                } else if (policyLevel == SearchRegionLevel.SIGUNGU && childOfSido(region, userRegionCode, userRegion.province())) {
                    childSigunguMatch = true;
                }
            }
        }
        if (exactSigunguMatch) {
            return result(multiple ? RegionCompatibility.MULTIPLE_SIGUNGU_MATCH : RegionCompatibility.EXACT_SIGUNGU,
                    true, 100, multiple ? "복수 지역 중 사용자 시·군·자치구를 포함합니다." : "사용자 시·군·자치구와 정확히 일치합니다.");
        }
        if (sidoMatch) {
            if (userLevel == SearchRegionLevel.SIGUNGU) {
                return result(multiple ? RegionCompatibility.MULTIPLE_SIDO_MATCH : RegionCompatibility.PARENT_SIDO,
                        true, 100, multiple ? "복수 지역 중 사용자 상위 시·도를 포함합니다." : "사용자 지역을 포함하는 상위 시·도 전체 정책입니다.");
            }
            return result(multiple ? RegionCompatibility.MULTIPLE_SIDO_MATCH : RegionCompatibility.EXACT_SIDO,
                    true, 100, multiple ? "복수 지역 중 사용자 시·도를 포함합니다." : "사용자 시·도 전체와 일치합니다.");
        }
        if (childSigunguMatch) {
            return result(multiple ? RegionCompatibility.MULTIPLE_CHILD_SIGUNGU_MATCH : RegionCompatibility.CHILD_SIGUNGU_MATCH,
                    true, 100, multiple ? "복수 지역 중 사용자 시·도 하위 지역을 포함합니다." : "사용자 시·도 하위 지역 정책입니다.");
        }
        if (nationwide) {
            return result(RegionCompatibility.NATIONWIDE, true, 100, "전국에서 신청 가능한 정책");
        }
        return result(RegionCompatibility.NOT_MATCHED, false, 0, "명확한 다른 지역 전용 정책입니다.");
    }

    private RegionMatchResult result(RegionCompatibility compatibility, boolean eligible, int score, String reason) {
        return new RegionMatchResult(compatibility, eligible, score, reason);
    }

    private boolean isNationwide(RegionCode region) {
        return SearchRegionLevel.from(region) == SearchRegionLevel.NATIONWIDE
                || "KR".equals(region.getRegionCode())
                || ("전국".equals(region.getProvince()) && !StringUtils.hasText(region.getCity()));
    }

    private boolean sameSigungu(RegionCode policyRegion, RegionCode userRegionCode, ResolvedUserRegion userRegion) {
        if (SearchRegionLevel.from(policyRegion) != SearchRegionLevel.SIGUNGU) {
            return false;
        }
        if (sameRegionIdentity(policyRegion, userRegionCode)) {
            return true;
        }
        return Objects.equals(policyRegion.getProvince(), userRegion.province())
                && Objects.equals(cityPart(policyRegion), userRegion.city());
    }

    private boolean sameSido(RegionCode policyRegion, RegionCode userSido, String userProvince) {
        if (SearchRegionLevel.from(policyRegion) != SearchRegionLevel.SIDO) {
            return false;
        }
        if (sameRegionIdentity(policyRegion, userSido)) {
            return true;
        }
        return Objects.equals(policyRegion.getProvince(), userProvince);
    }

    private boolean childOfSido(RegionCode policyRegion, RegionCode userSido, String userProvince) {
        if (SearchRegionLevel.from(policyRegion) != SearchRegionLevel.SIGUNGU) {
            return false;
        }
        RegionCode parent = policyRegion.getParent();
        if (sameRegionIdentity(parent, userSido)) {
            return true;
        }
        return Objects.equals(policyRegion.getProvince(), userProvince);
    }

    private boolean sameRegionIdentity(RegionCode left, RegionCode right) {
        if (left == null || right == null) {
            return false;
        }
        if (left.getId() != null && right.getId() != null) {
            return Objects.equals(left.getId(), right.getId());
        }
        if (StringUtils.hasText(left.getRegionCode()) && StringUtils.hasText(right.getRegionCode())) {
            return Objects.equals(left.getRegionCode(), right.getRegionCode());
        }
        return Objects.equals(left.getProvince(), right.getProvince())
                && Objects.equals(left.getCity(), right.getCity())
                && Objects.equals(left.getRegionLevel(), right.getRegionLevel());
    }

    private RegionCode parentSido(RegionCode region, ResolvedUserRegion userRegion) {
        if (region != null && region.getParent() != null && SearchRegionLevel.from(region.getParent()) == SearchRegionLevel.SIDO) {
            return region.getParent();
        }
        return catalog.findProvince(userRegion.province()).orElse(null);
    }

    private String cityPart(RegionCode region) {
        if (region.getCity() == null) {
            return null;
        }
        int idx = region.getCity().indexOf(' ');
        return idx > 0 ? region.getCity().substring(0, idx) : region.getCity();
    }

    private String emptyToNull(String value) {
        return StringUtils.hasText(value) ? value : null;
    }
}
