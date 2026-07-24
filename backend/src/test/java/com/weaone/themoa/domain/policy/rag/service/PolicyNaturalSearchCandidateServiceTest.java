package com.weaone.themoa.domain.policy.rag.service;

import com.weaone.themoa.domain.policy.policy.entity.RegionCode;
import com.weaone.themoa.domain.policy.policy.region.RegionCatalog;
import com.weaone.themoa.domain.policy.policy.region.RegionCompatibility;
import com.weaone.themoa.domain.policy.policy.region.RegionEligiblePolicyCandidate;
import com.weaone.themoa.domain.policy.policy.region.ResolvedUserRegion;
import com.weaone.themoa.domain.policy.policy.region.SearchRegionLevel;
import com.weaone.themoa.domain.policy.policy.repository.RegionEligiblePolicyCandidateRepository;
import com.weaone.themoa.domain.policy.policy.service.RegionEligiblePolicyCandidateService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.groups.Tuple.tuple;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class PolicyNaturalSearchCandidateServiceTest {
    private final RegionEligiblePolicyCandidateRepository repository =
            mock(RegionEligiblePolicyCandidateRepository.class);
    private final RegionCatalog catalog = mock(RegionCatalog.class);
    private final RegionEligiblePolicyCandidateService service =
            new RegionEligiblePolicyCandidateService(repository, catalog);

    @Test
    void naturalSearchCandidatesIncludeBestLocalSidoNationwideAndUnspecifiedPolicies() {
        RegionCode nationwide = region(1, null, "KR", "전국", null, "NATIONWIDE");
        RegionCode daegu = region(2, null, "P:27", "대구광역시", null, "PROVINCE");
        RegionCode dalseo = region(3, daegu, "M:27290", "대구광역시", "달서구", "DISTRICT");
        RegionCode seoul = region(4, null, "P:11", "서울특별시", null, "PROVINCE");
        ResolvedUserRegion userRegion =
                new ResolvedUserRegion("대구광역시", "달서구", null, SearchRegionLevel.SIGUNGU, dalseo);
        given(catalog.nationwide()).willReturn(Optional.of(nationwide));
        given(catalog.allRegions()).willReturn(List.of(nationwide, daegu, dalseo, seoul));
        given(repository.findEligibleRegionRows(anyList()))
                .willReturn(List.of(
                        row(10, dalseo.getId(), 1),
                        row(11, dalseo.getId(), 2),
                        row(12, daegu.getId(), 1),
                        row(13, daegu.getId(), 2),
                        row(14, nationwide.getId(), 1)
                ));
        given(repository.findRegionUnspecifiedPolicyIds()).willReturn(List.of(15));

        List<RegionEligiblePolicyCandidate> candidates = service.findSearchEligibleCandidates(userRegion);

        ArgumentCaptor<List> regionIdsCaptor = ArgumentCaptor.forClass(List.class);
        verify(repository).findEligibleRegionRows(regionIdsCaptor.capture());
        assertThat(candidates)
                .extracting(RegionEligiblePolicyCandidate::policyId, RegionEligiblePolicyCandidate::compatibility)
                .containsExactly(
                        tuple(10, RegionCompatibility.EXACT_SIGUNGU),
                        tuple(11, RegionCompatibility.MULTIPLE_SIGUNGU_MATCH),
                        tuple(12, RegionCompatibility.PARENT_SIDO),
                        tuple(13, RegionCompatibility.MULTIPLE_SIDO_MATCH),
                        tuple(14, RegionCompatibility.NATIONWIDE),
                        tuple(15, RegionCompatibility.REGION_UNSPECIFIED)
                );
    }

    @Test
    void naturalSearchKeepsBestCompatibilityWhenPolicyHasSeveralMatchingRows() {
        RegionCode nationwide = region(1, null, "KR", "전국", null, "NATIONWIDE");
        RegionCode daegu = region(2, null, "P:27", "대구광역시", null, "PROVINCE");
        RegionCode dalseo = region(3, daegu, "M:27290", "대구광역시", "달서구", "DISTRICT");
        ResolvedUserRegion userRegion =
                new ResolvedUserRegion("대구광역시", "달서구", null, SearchRegionLevel.SIGUNGU, dalseo);
        given(catalog.nationwide()).willReturn(Optional.of(nationwide));
        given(catalog.allRegions()).willReturn(List.of(nationwide, daegu, dalseo));
        given(repository.findEligibleRegionRows(anyList()))
                .willReturn(List.of(
                        row(20, daegu.getId(), 3),
                        row(20, nationwide.getId(), 3),
                        row(20, dalseo.getId(), 3)
                ));
        given(repository.findRegionUnspecifiedPolicyIds()).willReturn(List.of());

        List<RegionEligiblePolicyCandidate> candidates = service.findSearchEligibleCandidates(userRegion);

        assertThat(candidates)
                .extracting(RegionEligiblePolicyCandidate::policyId, RegionEligiblePolicyCandidate::compatibility)
                .containsExactly(tuple(20, RegionCompatibility.MULTIPLE_SIGUNGU_MATCH));
    }

    @Test
    void sidoNaturalSearchCandidatesIncludeSidoChildrenAndClassifyChildPolicies() {
        RegionCode nationwide = region(1, null, "KR", "전국", null, "NATIONWIDE");
        RegionCode daegu = region(2, null, "P:27", "대구광역시", null, "PROVINCE");
        RegionCode dalseo = region(3, daegu, "M:27290", "대구광역시", "달서구", "DISTRICT");
        RegionCode suseong = region(4, daegu, "M:27260", "대구광역시", "수성구", "DISTRICT");
        RegionCode seoul = region(5, null, "P:11", "서울특별시", null, "PROVINCE");
        RegionCode gangnam = region(6, seoul, "M:11680", "서울특별시", "강남구", "DISTRICT");
        ResolvedUserRegion userRegion =
                new ResolvedUserRegion("대구광역시", null, null, SearchRegionLevel.SIDO, daegu);
        given(catalog.nationwide()).willReturn(Optional.of(nationwide));
        given(catalog.allRegions()).willReturn(List.of(nationwide, daegu, dalseo, suseong, seoul, gangnam));
        given(repository.findEligibleRegionRows(anyList()))
                .willReturn(List.of(
                        row(30, daegu.getId(), 1),
                        row(31, dalseo.getId(), 1),
                        row(32, suseong.getId(), 2),
                        row(33, daegu.getId(), 2),
                        row(34, nationwide.getId(), 1)
                ));
        given(repository.findRegionUnspecifiedPolicyIds()).willReturn(List.of(35));

        List<RegionEligiblePolicyCandidate> candidates = service.findSearchEligibleCandidates(userRegion);

        assertThat(candidates)
                .extracting(RegionEligiblePolicyCandidate::policyId, RegionEligiblePolicyCandidate::compatibility)
                .containsExactly(
                        tuple(30, RegionCompatibility.EXACT_SIDO),
                        tuple(31, RegionCompatibility.CHILD_SIGUNGU_MATCH),
                        tuple(32, RegionCompatibility.MULTIPLE_CHILD_SIGUNGU_MATCH),
                        tuple(33, RegionCompatibility.MULTIPLE_SIDO_MATCH),
                        tuple(34, RegionCompatibility.NATIONWIDE),
                        tuple(35, RegionCompatibility.REGION_UNSPECIFIED)
                );
        assertThat(regionIdsCaptor.getValue())
                .contains(daegu.getId(), dalseo.getId(), suseong.getId(), nationwide.getId())
                .doesNotContain(seoul.getId(), gangnam.getId());
    }

    private Object[] row(Integer policyId, Integer regionId, int regionCount) {
        return new Object[]{policyId, regionId, regionCount};
    }

    private RegionCode region(Integer id, RegionCode parent, String code, String province, String city, String level) {
        RegionCode region = new RegionCode(parent, code, province, city, level);
        ReflectionTestUtils.setField(region, "id", id);
        return region;
    }
}
