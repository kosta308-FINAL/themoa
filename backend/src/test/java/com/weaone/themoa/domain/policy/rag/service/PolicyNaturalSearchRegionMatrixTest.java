package com.weaone.themoa.domain.policy.rag.service;

import com.weaone.themoa.domain.policy.policy.entity.Policy;
import com.weaone.themoa.domain.policy.policy.entity.PolicyCategory;
import com.weaone.themoa.domain.policy.policy.entity.PolicyRegion;
import com.weaone.themoa.domain.policy.policy.entity.RegionCode;
import com.weaone.themoa.domain.policy.policy.region.FakeRegionData;
import com.weaone.themoa.domain.policy.policy.region.RegionAliasCatalog;
import com.weaone.themoa.domain.policy.policy.region.RegionCatalog;
import com.weaone.themoa.domain.policy.policy.region.RegionCompatibility;
import com.weaone.themoa.domain.policy.policy.region.RegionMatchEvaluator;
import com.weaone.themoa.domain.policy.policy.region.RegionNormalizer;
import com.weaone.themoa.domain.policy.policy.region.ResolvedUserRegion;
import com.weaone.themoa.domain.policy.policy.region.SearchRegionLevel;
import com.weaone.themoa.domain.policy.policy.repository.RegionCodeRepository;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PolicyNaturalSearchRegionMatrixTest {
    private final RegionMatchEvaluator evaluator = evaluator();

    @Test
    void allSigunguRegionsPreferSigunguThenParentSidoThenNationwideAndExcludeOtherSido() {
        List<RegionCode> regions = FakeRegionData.regions();
        RegionCode nationwide = region("KR");

        assertThat(regions)
                .filteredOn(region -> SearchRegionLevel.from(region) == SearchRegionLevel.SIGUNGU)
                .allSatisfy(sigungu -> {
                    ResolvedUserRegion user = evaluator.resolveUserRegion(sigungu.getProvince(), cityPart(sigungu), null);
                    RegionCode parentSido = regions.stream()
                            .filter(region -> SearchRegionLevel.from(region) == SearchRegionLevel.SIDO)
                            .filter(region -> region.getProvince().equals(sigungu.getProvince()))
                            .findFirst()
                            .orElseThrow();
                    RegionCode otherSido = regions.stream()
                            .filter(region -> SearchRegionLevel.from(region) == SearchRegionLevel.SIDO)
                            .filter(region -> !region.getProvince().equals(sigungu.getProvince()))
                            .findFirst()
                            .orElseThrow();

                    assertThat(evaluator.evaluate(policy(sigungu), user).compatibility())
                            .isEqualTo(RegionCompatibility.EXACT_SIGUNGU);
                    assertThat(evaluator.evaluate(policy(sigungu, otherSido), user).compatibility())
                            .isEqualTo(RegionCompatibility.MULTIPLE_SIGUNGU_MATCH);
                    assertThat(evaluator.evaluate(policy(parentSido), user).compatibility())
                            .isEqualTo(RegionCompatibility.PARENT_SIDO);
                    assertThat(evaluator.evaluate(policy(parentSido, otherSido), user).compatibility())
                            .isEqualTo(RegionCompatibility.MULTIPLE_SIDO_MATCH);
                    assertThat(evaluator.evaluate(policy(nationwide), user).compatibility())
                            .isEqualTo(RegionCompatibility.NATIONWIDE);
                    assertThat(evaluator.evaluate(policy(otherSido), user).compatibility())
                            .isEqualTo(RegionCompatibility.NOT_MATCHED);
                });
    }

    @Test
    void sidoOnlyRegionsPreferSidoThenNationwide() {
        List<RegionCode> regions = FakeRegionData.regions();
        RegionCode nationwide = region("KR");

        assertThat(regions)
                .filteredOn(region -> SearchRegionLevel.from(region) == SearchRegionLevel.SIDO)
                .allSatisfy(sido -> {
                    ResolvedUserRegion user = evaluator.resolveUserRegion(sido.getProvince(), null, null);
                    List<RegionCode> childRegions = regions.stream()
                            .filter(region -> SearchRegionLevel.from(region) == SearchRegionLevel.SIGUNGU)
                            .filter(region -> region.getProvince().equals(sido.getProvince()))
                            .toList();
                    RegionCode otherSido = regions.stream()
                            .filter(region -> SearchRegionLevel.from(region) == SearchRegionLevel.SIDO)
                            .filter(region -> !region.getProvince().equals(sido.getProvince()))
                            .findFirst()
                            .orElseThrow();

                    assertThat(evaluator.evaluate(policy(sido), user).compatibility())
                            .isEqualTo(RegionCompatibility.EXACT_SIDO);
                    assertThat(evaluator.evaluate(policy(sido, otherSido), user).compatibility())
                            .isEqualTo(RegionCompatibility.MULTIPLE_SIDO_MATCH);
                    if (!childRegions.isEmpty()) {
                        RegionCode firstChild = childRegions.get(0);
                        assertThat(evaluator.evaluate(policy(firstChild), user).compatibility())
                                .isEqualTo(RegionCompatibility.CHILD_SIGUNGU_MATCH);
                        if (childRegions.size() > 1) {
                            assertThat(evaluator.evaluate(policy(firstChild, childRegions.get(1)), user).compatibility())
                                    .isEqualTo(RegionCompatibility.MULTIPLE_CHILD_SIGUNGU_MATCH);
                        }
                        assertThat(evaluator.evaluate(policy(firstChild, otherSido), user).compatibility())
                                .isEqualTo(RegionCompatibility.MULTIPLE_CHILD_SIGUNGU_MATCH);
                    }
                    assertThat(evaluator.evaluate(policy(nationwide), user).compatibility())
                            .isEqualTo(RegionCompatibility.NATIONWIDE);
                    assertThat(evaluator.evaluate(policy(otherSido), user).compatibility())
                            .isEqualTo(RegionCompatibility.NOT_MATCHED);
                });
    }

    private RegionMatchEvaluator evaluator() {
        RegionCodeRepository repository = mock(RegionCodeRepository.class);
        List<RegionCode> regions = FakeRegionData.regions();
        when(repository.findAll()).thenReturn(regions);
        for (RegionCode region : regions) {
            when(repository.findByRegionCode(region.getRegionCode())).thenReturn(Optional.of(region));
        }
        RegionAliasCatalog aliases = new RegionAliasCatalog();
        RegionNormalizer normalizer = new RegionNormalizer(aliases);
        return new RegionMatchEvaluator(new RegionCatalog(repository, aliases, normalizer), normalizer);
    }

    private Policy policy(RegionCode... regions) {
        Policy policy = new Policy("P" + Math.random());
        policy.updateBasic("정책", "기관", PolicyCategory.복지, "요약", null, null, null, true, true, "OPEN");
        for (RegionCode region : regions) {
            policy.getRegions().add(new PolicyRegion(policy, region));
        }
        return policy;
    }

    private RegionCode region(String code) {
        return FakeRegionData.regions().stream()
                .filter(region -> code.equals(region.getRegionCode()))
                .findFirst()
                .orElseThrow();
    }

    private String cityPart(RegionCode region) {
        if (region.getCity() == null) {
            return null;
        }
        int index = region.getCity().indexOf(' ');
        return index > 0 ? region.getCity().substring(0, index) : region.getCity();
    }
}
