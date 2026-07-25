package com.weaone.themoa.domain.policy.policy.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.weaone.themoa.domain.policy.policy.entity.Policy;
import com.weaone.themoa.domain.policy.policy.entity.PolicyCategory;
import com.weaone.themoa.domain.policy.policy.entity.PolicyRegion;
import com.weaone.themoa.domain.policy.policy.entity.PolicySourceSnapshot;
import com.weaone.themoa.domain.policy.policy.entity.RegionCode;
import com.weaone.themoa.domain.policy.policy.region.InstitutionRegionResolver;
import com.weaone.themoa.domain.policy.policy.region.PolicyGeographyClassifier;
import com.weaone.themoa.domain.policy.policy.region.RegionAliasCatalog;
import com.weaone.themoa.domain.policy.policy.region.RegionCatalog;
import com.weaone.themoa.domain.policy.policy.region.RegionCompatibility;
import com.weaone.themoa.domain.policy.policy.region.RegionEligiblePolicyCandidate;
import com.weaone.themoa.domain.policy.policy.region.RegionNameAliasGenerator;
import com.weaone.themoa.domain.policy.policy.region.RegionNormalizer;
import com.weaone.themoa.domain.policy.policy.region.ResolvedUserRegion;
import com.weaone.themoa.domain.policy.policy.region.SearchRegionLevel;
import com.weaone.themoa.domain.policy.policy.region.StrictPolicyRegionMentionExtractor;
import com.weaone.themoa.domain.policy.policy.repository.PolicyEmbeddingSyncRepository;
import com.weaone.themoa.domain.policy.policy.repository.PolicyRegionClassificationRepository;
import com.weaone.themoa.domain.policy.policy.repository.PolicyRegionRepository;
import com.weaone.themoa.domain.policy.policy.repository.PolicyRepository;
import com.weaone.themoa.domain.policy.policy.repository.PolicySourceSnapshotRepository;
import com.weaone.themoa.domain.policy.policy.repository.RegionCodeRepository;
import com.weaone.themoa.domain.policy.policy.repository.RegionEligiblePolicyCandidateRepository;
import com.weaone.themoa.domain.policy.rag.service.PolicyDocumentBuilder;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.groups.Tuple.tuple;
import static org.mockito.Mockito.mock;

@DataJpaTest
class PolicyRegionClassificationPersistenceTest {
    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private PolicyRepository policyRepository;

    @Autowired
    private PolicyRegionRepository policyRegionRepository;

    @Autowired
    private PolicyRegionClassificationRepository classificationRepository;

    @Autowired
    private PolicySourceSnapshotRepository snapshotRepository;

    @Autowired
    private PolicyEmbeddingSyncRepository syncRepository;

    @Autowired
    private RegionCodeRepository regionCodeRepository;

    @Autowired
    private RegionEligiblePolicyCandidateRepository candidateRepository;

    @Test
    void sourceSnapshotSigunguEvidenceIsPersistedAsSpecificPolicyRegionAndExcludedForOtherSigungu() {
        RegionCode nationwide = persistRegion(null, "KR", "전국", null, "NATIONWIDE");
        RegionCode gyeonggi = persistRegion(null, "P:41", "경기도", null, "PROVINCE");
        RegionCode yangju = persistRegion(gyeonggi, "M:41630", "경기도", "양주시", "CITY");
        RegionCode pyeongtaek = persistRegion(gyeonggi, "M:41220", "경기도", "평택시", "CITY");
        Policy policy = persistPolicy("REGION-PERSIST-1", "2026년 평택시 청년창업 금융지원사업 이차보전 지원");
        snapshotRepository.save(new PolicySourceSnapshot(
                policy,
                null,
                "YOUTH_CENTER",
                policy.getSourcePolicyId(),
                """
                        {
                          "plcyNm": "2026년 평택시 청년창업 금융지원사업 이차보전 지원",
                          "ptcpPrpTrgtCn": "평택시 청년 창업자",
                          "plcyExplnCn": "청년창업 금융지원사업"
                        }
                        """,
                "0123456789012345678901234567890123456789012345678901234567890123"
        ));
        PolicyApplicabilityClassificationService classificationService = classificationService();

        classificationService.classifyFromSnapshot(policy, false);
        entityManager.flush();
        entityManager.clear();

        Policy reloaded = policyRepository.findWithRelationsByIdIn(List.of(policy.getId())).get(0);
        assertThat(policyRegionRepository.findByPolicy(reloaded))
                .extracting(region -> region.getRegion().getRegionCode(), region -> region.getRegion().getRegionLevel())
                .containsExactly(tuple(pyeongtaek.getRegionCode(), "CITY"));
        assertThat(policyRegionRepository.findByPolicy(reloaded))
                .extracting(PolicyRegion::getRegion)
                .extracting(RegionCode::getRegionCode)
                .doesNotContain(gyeonggi.getRegionCode(), nationwide.getRegionCode());

        RegionEligiblePolicyCandidateService candidateService = new RegionEligiblePolicyCandidateService(
                candidateRepository,
                regionCatalog()
        );
        List<RegionEligiblePolicyCandidate> candidates = candidateService.findRecommendationEligibleCandidates(
                new ResolvedUserRegion("경기도", "양주시", null, SearchRegionLevel.SIGUNGU, yangju)
        );
        assertThat(candidates)
                .extracting(RegionEligiblePolicyCandidate::policyId, RegionEligiblePolicyCandidate::compatibility)
                .doesNotContain(tuple(policy.getId(), RegionCompatibility.PARENT_SIDO))
                .doesNotContain(tuple(policy.getId(), RegionCompatibility.EXACT_SIGUNGU));
    }

    private PolicyApplicabilityClassificationService classificationService() {
        ObjectMapper objectMapper = new ObjectMapper();
        PolicyRegionClassificationStore store = new PolicyRegionClassificationStore(classificationRepository, objectMapper);
        ReflectionTestUtils.setField(store, "entityManager", entityManager.getEntityManager());
        return new PolicyApplicabilityClassificationService(
                geographyClassifier(),
                new PolicyRegionSyncService(policyRegionRepository, regionCodeRepository),
                store,
                snapshotRepository,
                syncRepository,
                mock(PolicyDocumentBuilder.class),
                objectMapper
        );
    }

    private PolicyGeographyClassifier geographyClassifier() {
        RegionCatalog catalog = regionCatalog();
        RegionAliasCatalog aliases = new RegionAliasCatalog();
        RegionNormalizer normalizer = new RegionNormalizer(aliases);
        StrictPolicyRegionMentionExtractor extractor = new StrictPolicyRegionMentionExtractor(
                catalog,
                new RegionNameAliasGenerator(),
                normalizer
        );
        return new PolicyGeographyClassifier(catalog, extractor, new InstitutionRegionResolver(catalog));
    }

    private RegionCatalog regionCatalog() {
        RegionAliasCatalog aliases = new RegionAliasCatalog();
        return new RegionCatalog(regionCodeRepository, aliases, new RegionNormalizer(aliases));
    }

    private RegionCode persistRegion(RegionCode parent, String code, String province, String city, String level) {
        RegionCode region = new RegionCode(parent, code, province, city, level);
        entityManager.persist(region);
        return region;
    }

    private Policy persistPolicy(String sourcePolicyId, String title) {
        Policy policy = new Policy(sourcePolicyId);
        policy.updateBasic(title, "기관", PolicyCategory.일자리, "요약",
                null, LocalDate.of(2026, 7, 1), LocalDate.of(2026, 8, 31),
                false, true, "OPEN");
        policyRepository.save(policy);
        return policy;
    }
}
