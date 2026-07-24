package com.weaone.themoa.domain.policy.policy.service;

import com.weaone.themoa.domain.policy.policy.entity.RegionCode;
import com.weaone.themoa.domain.policy.policy.region.RegionCatalog;
import com.weaone.themoa.domain.policy.policy.region.RegionCompatibility;
import com.weaone.themoa.domain.policy.policy.region.RegionEligiblePolicyCandidate;
import com.weaone.themoa.domain.policy.policy.region.ResolvedUserRegion;
import com.weaone.themoa.domain.policy.policy.region.SearchRegionLevel;
import com.weaone.themoa.domain.policy.policy.repository.RegionEligiblePolicyCandidateRepository;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class RegionEligiblePolicyCandidateService {
    private final RegionEligiblePolicyCandidateRepository repository;
    private final RegionCatalog catalog;

    public RegionEligiblePolicyCandidateService(RegionEligiblePolicyCandidateRepository repository, RegionCatalog catalog) {
        this.repository = repository;
        this.catalog = catalog;
    }

    public List<RegionEligiblePolicyCandidate> findEligibleCandidates(ResolvedUserRegion userRegion) {
        return findEligibleCandidates(userRegion, false, false);
    }

    public List<RegionEligiblePolicyCandidate> findSearchEligibleCandidates(ResolvedUserRegion userRegion) {
        return findEligibleCandidates(userRegion, true, true);
    }

    public List<RegionEligiblePolicyCandidate> findRecommendationEligibleCandidates(ResolvedUserRegion userRegion) {
        return findEligibleCandidates(userRegion, true, false);
    }

    private List<RegionEligiblePolicyCandidate> findEligibleCandidates(ResolvedUserRegion userRegion,
                                                                       boolean includeRegionUnspecified,
                                                                       boolean includeSidoChildren) {
        if (userRegion == null || !userRegion.hasRegion() || userRegion.region() == null) {
            return List.of();
        }
        Integer userRegionId = userRegion.region().getId();
        Integer nationwideId = catalog.nationwide().map(RegionCode::getId).orElse(null);
        if (userRegionId == null || nationwideId == null) {
            return List.of();
        }
        Set<Integer> childIds = childSigunguIds(userRegion, includeSidoChildren);
        Set<Integer> exactIds = exactRegionIds(userRegion, childIds);
        Set<Integer> parentIds = parentSidoIds(userRegion);
        Set<Integer> nationwideIds = nationwideId == null ? Set.of() : Set.of(nationwideId);
        LinkedHashSet<Integer> eligibleRegionIds = new LinkedHashSet<>();
        eligibleRegionIds.addAll(exactIds);
        if (userRegion.level() == SearchRegionLevel.SIGUNGU) {
            eligibleRegionIds.addAll(parentIds);
        }
        eligibleRegionIds.addAll(nationwideIds);
        if (eligibleRegionIds.isEmpty()) {
            return List.of();
        }
        List<Object[]> rows = repository.findEligibleRegionRows(eligibleRegionIds.stream().toList());
        Map<Integer, RegionCompatibility> best = new LinkedHashMap<>();
        for (Object[] row : rows) {
            Integer policyId = ((Number) row[0]).intValue();
            Integer regionId = ((Number) row[1]).intValue();
            int regionCount = ((Number) row[2]).intValue();
            RegionCompatibility compatibility = compatibility(regionId, regionCount, userRegion, exactIds, childIds, parentIds, nationwideIds);
            best.merge(policyId, compatibility, (left, right) -> moreSpecific(left, right, userRegion.level()));
        }
        if (includeRegionUnspecified) {
            repository.findRegionUnspecifiedPolicyIds().forEach(policyId ->
                    best.putIfAbsent(policyId, RegionCompatibility.REGION_UNSPECIFIED));
        }
        return best.entrySet().stream()
                .map(entry -> new RegionEligiblePolicyCandidate(entry.getKey(), entry.getValue()))
                .toList();
    }

    private Set<Integer> exactRegionIds(ResolvedUserRegion userRegion, Set<Integer> childIds) {
        if (userRegion.level() == SearchRegionLevel.SIGUNGU) {
            return catalog.allRegions().stream()
                    .filter(region -> java.util.Objects.equals(region.getProvince(), userRegion.region().getProvince()))
                    .filter(region -> java.util.Objects.equals(region.getCity(), userRegion.region().getCity()))
                    .map(RegionCode::getId)
                    .filter(java.util.Objects::nonNull)
                    .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));
        }
        if (userRegion.level() == SearchRegionLevel.SIDO) {
            LinkedHashSet<Integer> ids = new LinkedHashSet<>(parentSidoIds(userRegion));
            ids.addAll(childIds);
            return ids;
        }
        return Set.of();
    }

    private Set<Integer> childSigunguIds(ResolvedUserRegion userRegion, boolean includeSidoChildren) {
        if (!includeSidoChildren || userRegion.level() != SearchRegionLevel.SIDO || userRegion.region() == null) {
            return Set.of();
        }
        return catalog.searchSupportedChildrenOf(userRegion.region()).stream()
                .map(RegionCode::getId)
                .filter(java.util.Objects::nonNull)
                .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));
    }

    private Set<Integer> parentSidoIds(ResolvedUserRegion userRegion) {
        String province = userRegion.region() == null ? userRegion.province() : userRegion.region().getProvince();
        return catalog.allRegions().stream()
                .filter(region -> "PROVINCE".equals(region.getRegionLevel()))
                .filter(region -> java.util.Objects.equals(region.getProvince(), province))
                .map(RegionCode::getId)
                .filter(java.util.Objects::nonNull)
                .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));
    }

    private RegionCompatibility compatibility(Integer regionId, int regionCount, ResolvedUserRegion userRegion,
                                              Set<Integer> exactIds, Set<Integer> childIds,
                                              Set<Integer> parentIds, Set<Integer> nationwideIds) {
        if (nationwideIds.contains(regionId)) {
            return RegionCompatibility.NATIONWIDE;
        }
        if (userRegion.level() == SearchRegionLevel.SIGUNGU && exactIds.contains(regionId)) {
            return regionCount > 1 ? RegionCompatibility.MULTIPLE_SIGUNGU_MATCH : RegionCompatibility.EXACT_SIGUNGU;
        }
        if (userRegion.level() == SearchRegionLevel.SIGUNGU && parentIds.contains(regionId)) {
            return regionCount > 1 ? RegionCompatibility.MULTIPLE_SIDO_MATCH : RegionCompatibility.PARENT_SIDO;
        }
        if (userRegion.level() == SearchRegionLevel.SIDO && exactIds.contains(regionId)) {
            if (parentIds.contains(regionId)) {
                return regionCount > 1 ? RegionCompatibility.MULTIPLE_SIDO_MATCH : RegionCompatibility.EXACT_SIDO;
            }
            if (childIds.contains(regionId)) {
                return regionCount > 1
                        ? RegionCompatibility.MULTIPLE_CHILD_SIGUNGU_MATCH
                        : RegionCompatibility.CHILD_SIGUNGU_MATCH;
            }
            return regionCount > 1 ? RegionCompatibility.MULTIPLE_SIDO_MATCH : RegionCompatibility.EXACT_SIDO;
        }
        return RegionCompatibility.UNKNOWN;
    }

    private RegionCompatibility moreSpecific(RegionCompatibility left, RegionCompatibility right, SearchRegionLevel userLevel) {
        return compatibilityPriority(left, userLevel) <= compatibilityPriority(right, userLevel) ? left : right;
    }

    private int compatibilityPriority(RegionCompatibility compatibility, SearchRegionLevel userLevel) {
        if (userLevel == SearchRegionLevel.SIDO) {
            return switch (compatibility) {
                case EXACT_SIDO -> 0;
                case CHILD_SIGUNGU_MATCH -> 1;
                case MULTIPLE_CHILD_SIGUNGU_MATCH -> 2;
                case MULTIPLE_SIDO_MATCH, MULTIPLE_REGION_MATCH -> 3;
                case NATIONWIDE -> 4;
                case REGION_UNSPECIFIED -> 5;
                case UNKNOWN -> 6;
                case EXACT_SIGUNGU, MULTIPLE_SIGUNGU_MATCH, PARENT_SIDO, NOT_MATCHED -> 7;
            };
        }
        return compatibility.priority();
    }
}
