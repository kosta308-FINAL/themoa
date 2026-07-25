package com.weaone.themoa.domain.policy.policy.region;

import com.weaone.themoa.domain.policy.policy.entity.RegionCode;
import com.weaone.themoa.domain.policy.policy.entity.RegionExternalCode;
import com.weaone.themoa.domain.policy.policy.repository.RegionCodeRepository;
import com.weaone.themoa.domain.policy.policy.repository.RegionExternalCodeRepository;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PolicyRegionResolverTest {
    private final RegionCodeRepository repository = repository();
    private final RegionExternalCodeRepository externalCodeRepository = externalCodeRepository();
    private final RegionAliasCatalog aliases = new RegionAliasCatalog();
    private final RegionNormalizer normalizer = new RegionNormalizer(aliases);
    private final RegionCatalog catalog = new RegionCatalog(repository, externalCodeRepository, aliases, normalizer);
    private final PolicyRegionResolver resolver = new PolicyRegionResolver(catalog, new InstitutionRegionResolver(catalog));

    @Test
    void noRegionEvidenceIsUnknown() {
        PolicyRegionResolution result = resolver.resolve(Map.of("plcyNm", "청년 자립 지원"));

        assertThat(result.scope()).isEqualTo(RegionScope.UNKNOWN);
        assertThat(result.regionCodes()).isEmpty();
    }

    @Test
    void explicitNationwideOnlyWhenTargetContextSaysSo() {
        assertThat(resolver.resolve(Map.of("ptcpPrpTrgtCn", "전국 거주 청년")).scope()).isEqualTo(RegionScope.NATIONWIDE);
        assertThat(resolver.resolve(Map.of("addAplyQlfcCndCn", "지역 제한 없음")).scope()).isEqualTo(RegionScope.NATIONWIDE);
    }

    @Test
    void extractsRegionFromPolicyTitle() {
        assertThat(resolver.resolve(Map.of("plcyNm", "서울시 청년수당")).regionNames()).contains("서울특별시");
        assertThat(resolver.resolve(Map.of("plcyNm", "부산 대학생 학자금 대출이자 지원")).regionNames()).contains("부산광역시");
        assertThat(resolver.resolve(Map.of("plcyNm", "대구 청년 취업 지원")).regionNames()).contains("대구광역시");
        assertThat(resolver.resolve(Map.of("plcyNm", "경북 청년애꿈 수당")).regionNames()).contains("경상북도");
        assertThat(resolver.resolve(Map.of("plcyNm", "2026년 청년도전지원사업(원주시)")).regionNames()).contains("강원특별자치도 원주시");
        assertThat(resolver.resolve(Map.of("plcyNm", "인천시 동구 청년월세 지원사업")).regionNames()).contains("인천광역시 동구");
        assertThat(resolver.resolve(Map.of("plcyNm", "서산시 청년면접수당")).regionNames()).contains("충청남도 서산시");
        assertThat(resolver.resolve(Map.of("plcyNm", "미추홀구 청년 면접수당")).regionNames()).contains("인천광역시 미추홀구");
        assertThat(resolver.resolve(Map.of("plcyNm", "남동구 청년월세")).regionNames()).contains("인천광역시 남동구");
        assertThat(resolver.resolve(Map.of("plcyNm", "부평구 청년도전")).regionNames()).contains("인천광역시 부평구");
        assertThat(resolver.resolve(Map.of("plcyNm", "평택시 청년 월세 지원")).regionNames()).contains("경기도 평택시");
        assertThat(resolver.resolve(Map.of("plcyNm", "2026년 평택시 청년창업 금융지원사업 이차보전 지원")).regionNames())
                .containsExactlyInAnyOrder("경기도 평택시");
        assertThat(resolver.resolve(Map.of("plcyNm", "양주시 청년 지원사업")).regionNames()).containsExactlyInAnyOrder("경기도 양주시");
        assertThat(resolver.resolve(Map.of("plcyNm", "원주시 청년도전지원사업")).regionNames()).containsExactlyInAnyOrder("강원특별자치도 원주시");
        assertThat(resolver.resolve(Map.of("plcyNm", "제주특별자치도 청년 지원")).regionNames()).containsExactlyInAnyOrder("제주특별자치도");
        assertThat(resolver.resolve(Map.of("plcyNm", "부산 대학생 지원")).regionNames()).containsExactlyInAnyOrder("부산광역시");
        assertThat(resolver.resolve(Map.of("plcyNm", "창원시 청년 지원")).regionNames()).contains("경상남도 창원시");
        assertThat(resolver.resolve(Map.of("plcyNm", "경기도 청년 면접수당")).regionNames()).contains("경기도");
        assertThat(resolver.resolve(Map.of("plcyNm", "제주도 청년 지원")).regionNames()).contains("제주특별자치도");
    }

    @Test
    void titleRegionWinsOverLowerPriorityContent() {
        PolicyRegionResolution result = resolver.resolve(Map.of(
                "plcyNm", "미추홀구 청년 면접수당",
                "plcySprtCn", "경기도 사례를 참고한 청년 지원 사업"
        ));

        assertThat(result.regionNames()).containsExactly("인천광역시 미추홀구");
    }

    @Test
    void extractsInstitutionRegionButIgnoresCentralInstitution() {
        assertThat(resolver.resolve(Map.of("sprvsnInstCdNm", "수원시청")).regionNames()).contains("경기도 수원시");
        assertThat(resolver.resolve(Map.of("sprvsnInstCdNm", "경기도일자리재단")).regionNames()).contains("경기도");
        assertThat(resolver.resolve(Map.of("sprvsnInstCdNm", "서울특별시 미래청년기획관")).regionNames()).contains("서울특별시");
        assertThat(resolver.resolve(Map.of("sprvsnInstCdNm", "제주특별자치도청")).regionNames()).contains("제주특별자치도");
        assertThat(resolver.resolve(Map.of("sprvsnInstCdNm", "고용노동부")).scope()).isEqualTo(RegionScope.UNKNOWN);
        assertThat(resolver.resolve(Map.of("sprvsnInstCdNm", "보건복지부")).scope()).isEqualTo(RegionScope.UNKNOWN);
    }

    @Test
    void centralInstitutionDoesNotPromoteContentToNationwideWithoutExplicitNationwideText() {
        PolicyRegionResolution result = resolver.resolve(Map.of(
                "plcyNm", "청년 취업 역량 강화 프로그램",
                "sprvsnInstCdNm", "고용노동부",
                "plcySprtCn", "취업 상담과 교육을 지원합니다."
        ));

        assertThat(result.scope()).isEqualTo(RegionScope.UNKNOWN);
        assertThat(result.nationwideExplicitlyConfirmed()).isFalse();
    }

    @Test
    void ambiguousSigunguTitleIsNotNationwide() {
        PolicyRegionResolution result = resolver.resolve(Map.of(
                "plcyNm", "(서구) 중소기업 재직청년 복지공유제",
                "sprvsnInstCdNm", "중앙행정기관"
        ));

        assertThat(result.scope()).isEqualTo(RegionScope.UNKNOWN);
        assertThat(result.nationwideExplicitlyConfirmed()).isFalse();
        assertThat(result.regionCodes()).isEmpty();
    }

    @Test
    void institutionCanResolveAmbiguousTitleSigungu() {
        assertThat(resolver.resolve(Map.of(
                "plcyNm", "(동구) 청년월세 지원",
                "sprvsnInstCdNm", "인천광역시 동구청"
        )).regionNames()).contains("인천광역시 동구");
        assertThat(resolver.resolve(Map.of(
                "plcyNm", "(동구) 청년월세 지원",
                "sprvsnInstCdNm", "대구광역시 동구청"
        )).regionNames()).contains("대구광역시 동구");
    }

    @Test
    void regionMatrixUsesKnownRegionCodesAndKeepsDuplicateSigunguAmbiguous() {
        List<RegionCode> regions = FakeRegionData.regions();

        assertThat(regions).extracting(RegionCode::getRegionCode).doesNotHaveDuplicates();
        assertThat(regions).filteredOn(region -> "NATIONWIDE".equals(region.getRegionLevel()))
                .extracting(RegionCode::getRegionCode)
                .containsExactly("KR");

        regions.stream()
                .filter(region -> "PROVINCE".equals(region.getRegionLevel()))
                .forEach(region -> assertThat(resolver.resolve(Map.of("plcyNm", region.getProvince() + " 청년 지원")).regionNames())
                        .contains(region.displayName()));
        regions.stream()
                .filter(region -> "CITY".equals(region.getRegionLevel()) || "DISTRICT".equals(region.getRegionLevel()))
                .forEach(region -> assertThat(resolver.resolve(Map.of("plcyNm",
                                region.getProvince() + " " + region.getCity() + " 청년 지원")).regionNames())
                        .contains(region.displayName()));

        Map<String, Long> duplicateSigunguNames = regions.stream()
                .filter(region -> region.getCity() != null)
                .collect(java.util.stream.Collectors.groupingBy(
                        RegionCode::getCity,
                        java.util.stream.Collectors.counting()
                ));
        duplicateSigunguNames.entrySet().stream()
                .filter(entry -> entry.getValue() > 1)
                .forEach(entry -> assertThat(resolver.resolve(Map.of("plcyNm", entry.getKey() + " 청년 지원")).scope())
                        .isEqualTo(RegionScope.UNKNOWN));
    }

    @Test
    void parsesZipCd() {
        assertThat(resolver.resolve(Map.of("zipCd", "41111,41113,41115,41117")).regionNames()).isEmpty();
        assertThat(resolver.resolve(Map.of("zipCd", "41111,41113,41115,41117")).evidence())
                .anyMatch(evidence -> evidence.source() == RegionEvidenceSource.ZIP_CODE);
        assertThat(resolver.resolve(Map.of("zipCd", "not-a-code")).scope()).isEqualTo(RegionScope.UNKNOWN);
    }

    private RegionCodeRepository repository() {
        RegionCodeRepository repo = mock(RegionCodeRepository.class);
        List<RegionCode> regions = FakeRegionData.regions();
        when(repo.findAll()).thenReturn(regions);
        for (RegionCode region : regions) {
            when(repo.findByRegionCode(region.getRegionCode())).thenReturn(Optional.of(region));
            when(repo.findByProvince(region.getProvince())).thenReturn(regions.stream().filter(r -> r.getProvince().equals(region.getProvince())).toList());
            when(repo.findByProvinceAndCity(region.getProvince(), region.getCity())).thenReturn(regions.stream()
                    .filter(r -> r.getProvince().equals(region.getProvince()) && java.util.Objects.equals(r.getCity(), region.getCity())).toList());
        }
        return repo;
    }

    private RegionExternalCodeRepository externalCodeRepository() {
        RegionExternalCodeRepository repo = mock(RegionExternalCodeRepository.class);
        RegionCode suwon = FakeRegionData.regions().stream()
                .filter(region -> "경기도".equals(region.getProvince()) && "수원시".equals(region.getCity()))
                .findFirst()
                .orElseThrow();
        for (String zipCd : List.of("41111", "41113", "41115", "41117")) {
            when(repo.findByCodeSystemAndExternalCode("YOUTH_CENTER_ZIP", zipCd))
                    .thenReturn(Optional.of(new RegionExternalCode(suwon, "YOUTH_CENTER_ZIP", zipCd)));
        }
        return repo;
    }
}
