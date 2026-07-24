package com.weaone.themoa.domain.policy.policy.repository;

import com.weaone.themoa.domain.policy.policy.entity.Policy;
import com.weaone.themoa.domain.policy.policy.entity.PolicyCategory;
import com.weaone.themoa.domain.policy.policy.entity.PolicyRegion;
import com.weaone.themoa.domain.policy.policy.entity.RegionCode;
import com.weaone.themoa.domain.policy.policy.region.RegionAliasCatalog;
import com.weaone.themoa.domain.policy.policy.region.RegionCatalog;
import com.weaone.themoa.domain.policy.policy.region.RegionCompatibility;
import com.weaone.themoa.domain.policy.policy.region.RegionEligiblePolicyCandidate;
import com.weaone.themoa.domain.policy.policy.region.RegionNormalizer;
import com.weaone.themoa.domain.policy.policy.region.ResolvedUserRegion;
import com.weaone.themoa.domain.policy.policy.region.SearchRegionLevel;
import com.weaone.themoa.domain.policy.policy.service.RegionEligiblePolicyCandidateService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.groups.Tuple.tuple;

@DataJpaTest
class RegionEligiblePolicyCandidateRepositoryJpaTest {
    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private RegionCodeRepository regionCodeRepository;

    @Autowired
    private RegionEligiblePolicyCandidateRepository candidateRepository;

    @Test
    void sidoSearchCandidateQueryIncludesSidoChildRegionsAndServiceDeduplicatesPolicies() {
        RegionCode nationwide = persistRegion(null, "KR", "전국", null, "NATIONWIDE");
        RegionCode daegu = persistRegion(null, "P:27", "대구광역시", null, "PROVINCE");
        RegionCode dalseo = persistRegion(daegu, "M:27290", "대구광역시", "달서구", "DISTRICT");
        RegionCode suseong = persistRegion(daegu, "M:27260", "대구광역시", "수성구", "DISTRICT");
        RegionCode gyeongbuk = persistRegion(null, "P:47", "경상북도", null, "PROVINCE");
        RegionCode seoul = persistRegion(null, "P:11", "서울특별시", null, "PROVINCE");
        RegionCode gangnam = persistRegion(seoul, "M:11680", "서울특별시", "강남구", "DISTRICT");

        Policy daeguPolicy = persistPolicy("DAEGU", daegu);
        Policy dalseoPolicy = persistPolicy("DALSEO", dalseo);
        Policy suseongPolicy = persistPolicy("SUSEONG", suseong);
        Policy multiChildPolicy = persistPolicy("DAEGU_CHILD_MULTI", dalseo, suseong);
        Policy multiSidoPolicy = persistPolicy("DAEGU_SIDO_MULTI", daegu, gyeongbuk);
        Policy nationwidePolicy = persistPolicy("NATIONWIDE", nationwide);
        Policy gangnamPolicy = persistPolicy("GANGNAM", gangnam);
        Policy unspecifiedPolicy = persistPolicy("UNSPECIFIED");
        entityManager.flush();
        entityManager.clear();

        List<Integer> eligibleIds = List.of(daegu.getId(), dalseo.getId(), suseong.getId(), nationwide.getId());
        List<Object[]> rows = candidateRepository.findEligibleRegionRows(eligibleIds);
        Set<Integer> rowPolicyIds = rows.stream()
                .map(row -> ((Number) row[0]).intValue())
                .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));

        assertThat(rowPolicyIds)
                .contains(
                        daeguPolicy.getId(),
                        dalseoPolicy.getId(),
                        suseongPolicy.getId(),
                        multiChildPolicy.getId(),
                        multiSidoPolicy.getId(),
                        nationwidePolicy.getId()
                )
                .doesNotContain(gangnamPolicy.getId());

        RegionEligiblePolicyCandidateService service = new RegionEligiblePolicyCandidateService(
                candidateRepository,
                new RegionCatalog(regionCodeRepository, new RegionAliasCatalog(), new RegionNormalizer(new RegionAliasCatalog()))
        );
        List<RegionEligiblePolicyCandidate> candidates = service.findSearchEligibleCandidates(
                new ResolvedUserRegion("대구광역시", null, null, SearchRegionLevel.SIDO, daegu)
        );

        assertThat(candidates)
                .extracting(RegionEligiblePolicyCandidate::policyId, RegionEligiblePolicyCandidate::compatibility)
                .contains(
                        tuple(daeguPolicy.getId(), RegionCompatibility.EXACT_SIDO),
                        tuple(dalseoPolicy.getId(), RegionCompatibility.CHILD_SIGUNGU_MATCH),
                        tuple(suseongPolicy.getId(), RegionCompatibility.CHILD_SIGUNGU_MATCH),
                        tuple(multiChildPolicy.getId(), RegionCompatibility.MULTIPLE_CHILD_SIGUNGU_MATCH),
                        tuple(multiSidoPolicy.getId(), RegionCompatibility.MULTIPLE_SIDO_MATCH),
                        tuple(nationwidePolicy.getId(), RegionCompatibility.NATIONWIDE),
                        tuple(unspecifiedPolicy.getId(), RegionCompatibility.REGION_UNSPECIFIED)
                );
        assertThat(candidates).extracting(RegionEligiblePolicyCandidate::policyId)
                .doesNotContain(gangnamPolicy.getId())
                .doesNotHaveDuplicates();
    }

    private RegionCode persistRegion(RegionCode parent, String code, String province, String city, String level) {
        RegionCode region = new RegionCode(parent, code, province, city, level);
        entityManager.persist(region);
        return region;
    }

    private Policy persistPolicy(String sourcePolicyId, RegionCode... regions) {
        Policy policy = new Policy(sourcePolicyId);
        policy.updateBasic(sourcePolicyId + " 정책", "기관", PolicyCategory.복지, "요약",
                null, null, null, true, true, "OPEN");
        for (RegionCode region : regions) {
            policy.getRegions().add(new PolicyRegion(policy, region));
        }
        entityManager.persist(policy);
        return policy;
    }
}
