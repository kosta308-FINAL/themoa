package com.weaone.themoa.domain.policy.policy.region;

import com.weaone.themoa.domain.policy.policy.repository.RegionCodeRepository;
import com.weaone.themoa.domain.policy.policy.repository.RegionExternalCodeRepository;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class StrictPolicyRegionMentionExtractorTest {
    private final RegionCodeRepository repository = mock(RegionCodeRepository.class);
    private final RegionExternalCodeRepository externalCodeRepository = mock(RegionExternalCodeRepository.class);
    private final RegionNameAliasGenerator aliasGenerator = new RegionNameAliasGenerator();
    private final RegionNormalizer normalizer = new RegionNormalizer(new RegionAliasCatalog(aliasGenerator));
    private final RegionCatalog catalog = new RegionCatalog(repository, externalCodeRepository, aliasGenerator == null ? null : new RegionAliasCatalog(aliasGenerator), normalizer);
    private final StrictPolicyRegionMentionExtractor extractor = new StrictPolicyRegionMentionExtractor(catalog, aliasGenerator, normalizer);

    StrictPolicyRegionMentionExtractorTest() {
        java.util.List<com.weaone.themoa.domain.policy.policy.entity.RegionCode> regions = FakeRegionData.regions();
        when(repository.findAll()).thenReturn(regions);
        regions.forEach(region -> when(repository.findByRegionCode(region.getRegionCode())).thenReturn(Optional.of(region)));
    }

    @Test
    void officialMunicipalityNameIsRecognizedButCommonNounWithoutSuffixIsNot() {
        assertThat(extractor.extractMentions("예산군 청년 지원", false))
                .anyMatch(mention -> "예산군".equals(mention.region().getCity())
                        && mention.role() == PolicyRegionMentionRole.REFERENCE_ONLY);
        assertThat(extractor.extract("사업 예산 확보 및 청년 역량 함양", false))
                .noneMatch(region -> "예산군".equals(region.getCity()) || "함양군".equals(region.getCity()));
    }

    @Test
    void shortAliasRequiresRegionContextInPolicySource() {
        assertThat(extractor.extract("수원 거주 청년", true))
                .anyMatch(region -> "수원시".equals(region.getCity()));
        assertThat(extractor.extract("수원 사례 연구", false))
                .noneMatch(region -> "수원시".equals(region.getCity()));
    }
}
