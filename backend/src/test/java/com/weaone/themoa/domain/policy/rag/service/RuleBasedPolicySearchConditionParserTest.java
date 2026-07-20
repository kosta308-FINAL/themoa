package com.weaone.themoa.domain.policy.rag.service;

import com.weaone.themoa.domain.policy.policy.entity.RegionCode;
import com.weaone.themoa.domain.policy.policy.region.FakeRegionData;
import com.weaone.themoa.domain.policy.policy.region.RegionAliasCatalog;
import com.weaone.themoa.domain.policy.policy.region.RegionCatalog;
import com.weaone.themoa.domain.policy.policy.region.RegionNormalizer;
import com.weaone.themoa.domain.policy.policy.region.UserRegionTextResolver;
import com.weaone.themoa.domain.policy.policy.repository.RegionCodeRepository;
import com.weaone.themoa.domain.policy.rag.dto.PolicySearchCondition;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class RuleBasedPolicySearchConditionParserTest {
    private final RuleBasedPolicySearchConditionParser parser = new RuleBasedPolicySearchConditionParser(
            resolver(), new UserEmploymentStatusDetector());

    @Test
    void extractsRegionAgeEmploymentAndKeywords() {
        PolicySearchCondition condition = parser.parseCondition("수원 사는 27살 무직 청년 지원금", 20);

        assertThat(condition.province()).isEqualTo("경기도");
        assertThat(condition.city()).isEqualTo("수원시");
        assertThat(condition.age()).isEqualTo(27);
        assertThat(condition.employmentStatus()).isEqualTo("UNEMPLOYED");
        assertThat(condition.keywords()).contains("청년", "지원금");
        assertThat(condition.keywords()).doesNotContain("취업", "구직");
    }

    @Test
    void extractsJejuProvince() {
        PolicySearchCondition condition = parser.parseCondition("제주도 청년 월세 지원", 10);

        assertThat(condition.province()).isEqualTo("제주특별자치도");
        assertThat(condition.city()).isNull();
        assertThat(condition.category()).isEqualTo("주거");
    }

    @Test
    void extractsSyncedCountyFromDynamicCatalog() {
        PolicySearchCondition condition = parser.parseCondition("칠곡에 살고 있는 30살 청년이 받을 수 있는 취업 관련 정책", 10);

        assertThat(condition.province()).isEqualTo("경상북도");
        assertThat(condition.city()).isEqualTo("칠곡군");
        assertThat(condition.age()).isEqualTo(30);
    }

    @Test
    void keepsWorkplaceOutOfResidenceAndStoresWorkplaceDiagnostics() {
        PolicySearchCondition condition = parser.parseCondition("서울 회사에 다니고 있지만 다른 직장으로 옮기려고 해", 20);

        assertThat(condition.province()).isNull();
        assertThat(condition.workplaceProvince()).isEqualTo("서울특별시");
    }

    @Test
    void separatesResidenceWorkplaceAndInfersHighSchoolThirdGradeAgeForDisplayOnly() {
        PolicySearchCondition commute = parser.parseCondition("수원에 살고 서울로 출근하는 29살 직장인이야", 20);
        assertThat(commute.province()).isEqualTo("경기도");
        assertThat(commute.city()).isEqualTo("수원시");
        assertThat(commute.workplaceProvince()).isEqualTo("서울특별시");

        PolicySearchCondition highSchool = parser.parseCondition("경기도에 사는 고3인데 취업이나 직업교육 관련 지원 정책이 궁금해", 20);
        assertThat(highSchool.province()).isEqualTo("경기도");
        assertThat(highSchool.age()).isNull();
        assertThat(highSchool.inferredAge()).isEqualTo(18);
        assertThat(highSchool.inferredAgeSource()).isEqualTo("고3");
        assertThat(highSchool.inferredMinimumAge()).isEqualTo(17);
        assertThat(highSchool.inferredMaximumAge()).isEqualTo(18);
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
