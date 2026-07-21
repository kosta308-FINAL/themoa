package com.weaone.themoa.domain.policy.rag.service;

import com.weaone.themoa.domain.policy.policy.entity.RegionCode;
import com.weaone.themoa.domain.policy.policy.region.FakeRegionData;
import com.weaone.themoa.domain.policy.policy.region.RegionAliasCatalog;
import com.weaone.themoa.domain.policy.policy.region.RegionCatalog;
import com.weaone.themoa.domain.policy.policy.region.RegionNormalizer;
import com.weaone.themoa.domain.policy.policy.region.UserRegionTextResolver;
import com.weaone.themoa.domain.policy.policy.repository.RegionCodeRepository;
import com.weaone.themoa.domain.policy.rag.dto.PolicySearchCondition;
import com.weaone.themoa.domain.policy.rag.dto.PolicySearchMode;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PolicySearchConditionValidatorTest {
    private final UserRegionTextResolver userRegionTextResolver = resolver();
    private final UserEmploymentStatusDetector employmentStatusDetector = new UserEmploymentStatusDetector();
    private final PolicySearchConditionValidator validator = new PolicySearchConditionValidator(
            new ExplicitConditionDetector(userRegionTextResolver, employmentStatusDetector),
            new PolicyKeywordExtractor(new PolicyKeywordSynonymCatalog(), new PolicyKeywordNormalizer()),
            userRegionTextResolver,
            employmentStatusDetector);

    @Test
    void keywordQueryDoesNotCreateHardConditions() {
        PolicySearchCondition parsed = new PolicySearchCondition(null, null, null, 19, "UNEMPLOYED", null,
                null, "일자리", Set.of(), Set.of("청년", "면접", "수당"), 20);

        PolicySearchCondition condition = validator.validate("청년 면접 수당", parsed, 20);

        assertThat(condition.searchMode()).isEqualTo(PolicySearchMode.KEYWORD);
        assertThat(condition.regionExplicit()).isFalse();
        assertThat(condition.ageExplicit()).isFalse();
        assertThat(condition.employmentExplicit()).isFalse();
        assertThat(condition.age()).isNull();
        assertThat(condition.employmentStatus()).isNull();
        assertThat(condition.keywords()).contains("청년", "면접수당");
        assertThat(condition.expandedKeywords()).contains("면접비", "면접 지원금");
    }

    @Test
    void regionAndKeywordQueryIsHybrid() {
        PolicySearchCondition parsed = new PolicySearchCondition("경기도", null, null, null, null, null,
                null, "일자리", Set.of(), Set.of("청년", "면접", "수당"), 20);

        PolicySearchCondition condition = validator.validate("경기도 청년 면접 수당", parsed, 20);

        assertThat(condition.searchMode()).isEqualTo(PolicySearchMode.HYBRID);
        assertThat(condition.regionExplicit()).isTrue();
        assertThat(condition.province()).isEqualTo("경기도");
        assertThat(condition.ageExplicit()).isFalse();
    }

    @Test
    void interviewAllowanceDoesNotExpandToGenericGrantOnlyBecauseItContainsAllowance() {
        PolicySearchCondition parsed = new PolicySearchCondition(null, null, null, null, null, null,
                null, null, Set.of(), Set.of(), 10);

        PolicySearchCondition condition = validator.validate("면접수당", parsed, 10);

        assertThat(condition.keywords()).contains("면접수당");
        assertThat(condition.keywords()).doesNotContain("지원금");
        assertThat(condition.expandedKeywords()).contains("면접비");
        assertThat(condition.expandedKeywords()).doesNotContain("지원금", "보조금", "장려금");
    }

    @Test
    void situationQueryKeepsExplicitConditions() {
        PolicySearchCondition parsed = new PolicySearchCondition("경기도", "수원시", null, 27, "UNEMPLOYED", null,
                null, "금융", Set.of("CASH"), Set.of("청년", "지원금"), 20);

        PolicySearchCondition condition = validator.validate("수원 사는 27살 무직 청년 지원금", parsed, 20);

        assertThat(condition.searchMode()).isEqualTo(PolicySearchMode.HYBRID);
        assertThat(condition.regionExplicit()).isTrue();
        assertThat(condition.ageExplicit()).isTrue();
        assertThat(condition.employmentExplicit()).isTrue();
        assertThat(condition.age()).isEqualTo(27);
        assertThat(condition.employmentStatus()).isEqualTo("UNEMPLOYED");
    }

    @Test
    void preservesDynamicCountyRegionFromQuery() {
        PolicySearchCondition parsed = new PolicySearchCondition(null, null, null, 30, null, null,
                null, "일자리", Set.of(), Set.of("청년", "취업"), 10);

        PolicySearchCondition condition = validator.validate("칠곡에 살고 있는 30살 청년이 받을 수 있는 취업 관련 정책", parsed, 10);

        assertThat(condition.regionExplicit()).isTrue();
        assertThat(condition.province()).isEqualTo("경상북도");
        assertThat(condition.city()).isEqualTo("칠곡군");
        assertThat(condition.age()).isEqualTo(30);
    }

    @Test
    void workplaceOnlyRegionDoesNotBecomeResidence() {
        PolicySearchCondition parsed = new PolicySearchCondition("서울특별시", null, null, null, "EMPLOYED", null,
                null, "일자리", Set.of(), Set.of("이직"), 10);

        PolicySearchCondition condition = validator.validate("서울 회사에 다니고 있지만 다른 직장으로 옮기려고 해", parsed, 10);

        assertThat(condition.regionExplicit()).isFalse();
        assertThat(condition.province()).isNull();
        assertThat(condition.workplaceProvince()).isEqualTo("서울특별시");
    }

    @Test
    void residenceOverridesWorkplaceRegion() {
        PolicySearchCondition parsed = new PolicySearchCondition("서울특별시", null, null, null, null, null,
                null, "일자리", Set.of(), Set.of("지역"), 10);

        PolicySearchCondition condition = validator.validate("서울에서 일하고 있지만 사는 곳은 인천이야", parsed, 10);

        assertThat(condition.regionExplicit()).isTrue();
        assertThat(condition.province()).isEqualTo("인천광역시");
        assertThat(condition.workplaceProvince()).isEqualTo("서울특별시");
    }

    private UserRegionTextResolver resolver() {
        RegionCodeRepository repository = mock(RegionCodeRepository.class);
        List<RegionCode> regions = FakeRegionData.regions();
        when(repository.findAll()).thenReturn(regions);
        for (RegionCode region : regions) {
            when(repository.findByRegionCode(region.getRegionCode())).thenReturn(Optional.of(region));
            when(repository.findByProvince(region.getProvince())).thenReturn(regions.stream()
                    .filter(candidate -> candidate.getProvince().equals(region.getProvince())).toList());
            if (region.getCity() != null) {
                when(repository.findByProvinceAndCity(region.getProvince(), region.getCity())).thenReturn(regions.stream()
                        .filter(candidate -> candidate.getProvince().equals(region.getProvince()) && region.getCity().equals(candidate.getCity()))
                        .toList());
            }
        }
        RegionAliasCatalog aliases = new RegionAliasCatalog();
        RegionNormalizer normalizer = new RegionNormalizer(aliases);
        return new UserRegionTextResolver(new RegionCatalog(repository, aliases, normalizer), aliases, normalizer);
    }
}
