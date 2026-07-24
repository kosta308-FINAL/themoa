package com.weaone.themoa.domain.policy.recommendation.service;

import com.weaone.themoa.domain.policy.policy.entity.RegionCode;
import com.weaone.themoa.domain.policy.policy.region.RegionCatalog;
import com.weaone.themoa.domain.policy.policy.region.RegionCompatibility;
import com.weaone.themoa.domain.policy.policy.region.RegionEligiblePolicyCandidate;
import com.weaone.themoa.domain.policy.policy.region.ResolvedUserRegion;
import com.weaone.themoa.domain.policy.policy.region.SearchRegionLevel;
import com.weaone.themoa.domain.policy.policy.repository.RegionEligiblePolicyCandidateRepository;
import com.weaone.themoa.domain.policy.policy.service.RegionEligiblePolicyCandidateService;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

class PolicyRecommendationCandidateRepositoryTest {
    private final RegionEligiblePolicyCandidateRepository repository =
            mock(RegionEligiblePolicyCandidateRepository.class);
    private final RegionCatalog catalog = mock(RegionCatalog.class);
    private final RegionEligiblePolicyCandidateService service =
            new RegionEligiblePolicyCandidateService(repository, catalog);

    @Test
    void candidateRowsKeepBestRegionCompatibilityPerPolicy() {
        RegionCode nationwide = region(1, null, "KR", "전국", null, "NATIONWIDE");
        RegionCode gyeonggi = region(2, null, "P:41", "경기도", null, "PROVINCE");
        RegionCode suwon = region(3, gyeonggi, "M:41110", "경기도", "수원시", "CITY");
        ResolvedUserRegion userRegion =
                new ResolvedUserRegion("경기도", "수원시", null, SearchRegionLevel.SIGUNGU, suwon);
        given(catalog.nationwide()).willReturn(Optional.of(nationwide));
        given(catalog.allRegions()).willReturn(List.of(nationwide, gyeonggi, suwon));
        given(repository.findEligibleRegionRows(anyList()))
                .willReturn(List.of(
                        row(10, suwon.getId(), 3),
                        row(10, gyeonggi.getId(), 3),
                        row(11, gyeonggi.getId(), 2),
                        row(12, nationwide.getId(), 1)
                ));
        given(repository.findRegionUnspecifiedPolicyIds()).willReturn(List.of(13));

        List<RegionEligiblePolicyCandidate> candidates = service.findRecommendationEligibleCandidates(userRegion);

        assertThat(candidates)
                .extracting(RegionEligiblePolicyCandidate::policyId, RegionEligiblePolicyCandidate::compatibility)
                .containsExactly(
                        org.assertj.core.groups.Tuple.tuple(10, RegionCompatibility.MULTIPLE_SIGUNGU_MATCH),
                        org.assertj.core.groups.Tuple.tuple(11, RegionCompatibility.MULTIPLE_SIDO_MATCH),
                        org.assertj.core.groups.Tuple.tuple(12, RegionCompatibility.NATIONWIDE),
                        org.assertj.core.groups.Tuple.tuple(13, RegionCompatibility.REGION_UNSPECIFIED)
                );
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
