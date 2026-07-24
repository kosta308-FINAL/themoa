package com.weaone.themoa.domain.policy.recommendation.service;

import com.weaone.themoa.domain.policy.policy.entity.RegionCode;
import com.weaone.themoa.domain.policy.policy.region.FakeRegionData;
import com.weaone.themoa.domain.policy.policy.region.RegionCompatibility;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

class PolicyRecommendationRegionMatrixTest {

    @Test
    void regionCompatibilityPriorityPlacesLocalRegionsBeforeNationwide() {
        assertThat(RegionCompatibility.EXACT_SIGUNGU.priority())
                .isLessThan(RegionCompatibility.MULTIPLE_SIGUNGU_MATCH.priority());
        assertThat(RegionCompatibility.MULTIPLE_SIGUNGU_MATCH.priority())
                .isLessThan(RegionCompatibility.EXACT_SIDO.priority());
        assertThat(RegionCompatibility.EXACT_SIDO.priority())
                .isEqualTo(RegionCompatibility.PARENT_SIDO.priority())
                .isLessThan(RegionCompatibility.MULTIPLE_SIDO_MATCH.priority());
        assertThat(RegionCompatibility.MULTIPLE_SIDO_MATCH.priority())
                .isLessThan(RegionCompatibility.NATIONWIDE.priority());
        assertThat(RegionCompatibility.NATIONWIDE.priority())
                .isLessThan(RegionCompatibility.REGION_UNSPECIFIED.priority());
        assertThat(RegionCompatibility.REGION_UNSPECIFIED.priority())
                .isLessThan(RegionCompatibility.UNKNOWN.priority());
    }

    @Test
    void fakeRegionMatrixHasNationwideAndUniqueRegionCodes() {
        List<RegionCode> regions = FakeRegionData.regions();

        assertThat(regions)
                .filteredOn(region -> "KR".equals(region.getRegionCode()))
                .hasSize(1);
        assertThat(regions)
                .extracting(RegionCode::getRegionCode)
                .doesNotHaveDuplicates();
    }

    @Test
    void everySigunguInMatrixHasExistingParentSidoByProvince() {
        List<RegionCode> regions = FakeRegionData.regions();
        Set<String> sidoNames = regions.stream()
                .filter(region -> "PROVINCE".equals(region.getRegionLevel()))
                .map(RegionCode::getProvince)
                .collect(Collectors.toCollection(HashSet::new));

        assertThat(regions)
                .filteredOn(region -> "CITY".equals(region.getRegionLevel()) || "DISTRICT".equals(region.getRegionLevel()))
                .allSatisfy(region -> assertThat(sidoNames).contains(region.getProvince()));
    }

    @Test
    void cityOptionalSidoRegionsRemainSelectableWithoutSigungu() {
        List<RegionCode> regions = FakeRegionData.regions();
        Set<String> provincesWithSigungu = regions.stream()
                .filter(region -> "CITY".equals(region.getRegionLevel()) || "DISTRICT".equals(region.getRegionLevel()))
                .map(RegionCode::getProvince)
                .collect(Collectors.toSet());

        assertThat(regions)
                .filteredOn(region -> "PROVINCE".equals(region.getRegionLevel()))
                .filteredOn(region -> !provincesWithSigungu.contains(region.getProvince()))
                .extracting(RegionCode::getCity)
                .containsOnlyNulls();
    }
}
