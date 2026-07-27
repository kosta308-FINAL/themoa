package com.weaone.themoa.domain.policy.recommendation.service;

import com.weaone.themoa.domain.member.entity.Gender;
import com.weaone.themoa.domain.member.entity.Member;
import com.weaone.themoa.domain.member.repository.MemberRepository;
import com.weaone.themoa.domain.policy.policy.entity.Policy;
import com.weaone.themoa.domain.policy.policy.entity.PolicyCategory;
import com.weaone.themoa.domain.policy.policy.entity.PolicyCondition;
import com.weaone.themoa.domain.policy.policy.entity.PolicyRegion;
import com.weaone.themoa.domain.policy.policy.entity.PolicyRegionClassification;
import com.weaone.themoa.domain.policy.policy.entity.PolicySearchProjection;
import com.weaone.themoa.domain.policy.policy.entity.RegionCode;
import com.weaone.themoa.domain.policy.policy.region.RegionAliasCatalog;
import com.weaone.themoa.domain.policy.policy.region.RegionCatalog;
import com.weaone.themoa.domain.policy.policy.region.RegionMatchEvaluator;
import com.weaone.themoa.domain.policy.policy.region.RegionNameAliasGenerator;
import com.weaone.themoa.domain.policy.policy.region.RegionNormalizer;
import com.weaone.themoa.domain.policy.policy.region.StrictPolicyRegionMentionExtractor;
import com.weaone.themoa.domain.policy.policy.repository.PolicyRepository;
import com.weaone.themoa.domain.policy.policy.repository.PolicyRegionClassificationRepository;
import com.weaone.themoa.domain.policy.policy.repository.PolicySearchProjectionRepository;
import com.weaone.themoa.domain.policy.policy.repository.RegionCodeRepository;
import com.weaone.themoa.domain.policy.policy.repository.RegionEligiblePolicyCandidateRepository;
import com.weaone.themoa.domain.policy.policy.repository.PolicyGenderClassificationRepository;
import com.weaone.themoa.domain.policy.rag.service.PolicyGenderAiAnalysis;
import com.weaone.themoa.domain.policy.rag.service.PolicyGenderAudienceClassifier;
import com.weaone.themoa.domain.policy.rag.service.PolicyGenderClassificationPolicy;
import com.weaone.themoa.domain.policy.rag.service.PolicyGenderSourceHasher;
import com.weaone.themoa.domain.policy.policy.service.RegionEligiblePolicyCandidateService;
import com.weaone.themoa.domain.policy.rag.service.PolicyEmploymentAudienceClassifier;
import com.weaone.themoa.domain.policy.rag.service.PolicyTargetAudienceClassifier;
import com.weaone.themoa.domain.policy.rag.service.PolicyTargetEligibilityFilter;
import com.weaone.themoa.domain.policy.recommendation.entity.MemberPolicyRecommendation;
import com.weaone.themoa.domain.policy.recommendation.entity.PolicyRecommendationProfile;
import com.weaone.themoa.domain.policy.recommendation.dto.response.PolicyRecommendationListResponse;
import com.weaone.themoa.domain.policy.recommendation.repository.MemberPolicyRecommendationRepository;
import com.weaone.themoa.domain.policy.recommendation.repository.PolicyRecommendationProfileRepository;
import com.weaone.themoa.domain.policy.rag.dto.UserEmploymentStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class PolicyRecommendationStrictEligibilityTest {
    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private PolicyRecommendationProfileRepository profileRepository;

    @Autowired
    private MemberPolicyRecommendationRepository recommendationRepository;

    @Autowired
    private PolicyRepository policyRepository;

    @Autowired
    private RegionCodeRepository regionCodeRepository;

    @Autowired
    private RegionEligiblePolicyCandidateRepository candidateRepository;

    @Autowired
    private PolicySearchProjectionRepository projectionRepository;

    @Autowired
    private PolicyRegionClassificationRepository regionClassificationRepository;

    @Autowired
    private PolicyGenderClassificationRepository genderClassificationRepository;

    private PolicyRecommendationService service;
    private PolicyGenderAudienceClassifier genderAudienceClassifier;
    private RegionCode nationwide;
    private RegionCode gyeonggi;
    private RegionCode yangju;
    private StrictPolicyRegionMentionExtractor regionMentionExtractor;

    @BeforeEach
    void setUp() {
        nationwide = persistRegion(null, "KR", "전국", null, "NATIONWIDE");
        gyeonggi = persistRegion(null, "P:41", "경기도", null, "PROVINCE");
        yangju = persistRegion(gyeonggi, "M:41630", "경기도", "양주시", "CITY");
        persistRegion(gyeonggi, "M:41220", "경기도", "평택시", "CITY");
        RegionAliasCatalog aliases = new RegionAliasCatalog();
        RegionNormalizer normalizer = new RegionNormalizer(aliases);
        RegionCatalog catalog = new RegionCatalog(regionCodeRepository, aliases, normalizer);
        regionMentionExtractor = new StrictPolicyRegionMentionExtractor(
                catalog,
                new RegionNameAliasGenerator(),
                normalizer
        );
        genderAudienceClassifier = new PolicyGenderAudienceClassifier(
                projectionRepository,
                genderClassificationRepository,
                projection -> {
                    if ("경력단절 및 경력보유 여성 구직자".equals(projection.getTargetText())) {
                        return new PolicyGenderAiAnalysis("FEMALE_ONLY", true, 0.95,
                                "INDIVIDUAL", "개인 신청 대상이 여성 구직자로 제한됩니다.");
                    }
                    if ("만 19~34세 청년 누구나".equals(projection.getTargetText())) {
                        return new PolicyGenderAiAnalysis("ALL", false, 0.90,
                                "INDIVIDUAL", "성별 제한 없이 청년 개인이 신청 가능합니다.");
                    }
                    return new PolicyGenderAiAnalysis("UNKNOWN", false, 0.20,
                            "UNKNOWN", "성별 제한 근거가 불명확합니다.");
                },
                new PolicyGenderClassificationPolicy(),
                new PolicyGenderSourceHasher()
        );
        service = new PolicyRecommendationService(
                memberRepository,
                profileRepository,
                recommendationRepository,
                policyRepository,
                new PolicyRecommendationAgeCalculator(),
                new PolicyRecommendationRegionService(catalog),
                new RegionEligiblePolicyCandidateService(candidateRepository, catalog),
                new PolicyEmploymentAudienceClassifier(projectionRepository),
                new PolicyTargetAudienceClassifier(projectionRepository),
                genderAudienceClassifier,
                new PolicyRecommendationMatcher(new PolicyTargetEligibilityFilter()),
                regionClassificationRepository,
                regionMentionExtractor,
                new RegionMatchEvaluator(catalog, normalizer)
        );
    }

    @Test
    void projectionBasedAutomaticRecommendationExcludesUnemployedOnlyPolicyForEmployedMember() {
        Member member = persistEmployedYangjuMember();
        Policy unemployedPolicy = persistPolicy("EMP-FLOW-1", "경기청년 맞춤형 채용지원 서비스", gyeonggi);
        persistProjection(
                unemployedPolicy,
                "도내 미취업 청년 대상",
                "미취업 상태의 청년",
                "기업과 일자리 매칭 후 일정 기간 근로 경험 제공"
        );
        Policy employedPolicy = persistPolicy("EMP-FLOW-2", "중소기업 재직청년 복지지원", gyeonggi);
        persistProjection(
                employedPolicy,
                "현재 중소기업에 재직 중인 청년",
                "",
                "청년 복지비를 지원합니다."
        );
        entityManager.flush();
        entityManager.clear();

        service.refreshForMember(member.getId());

        List<MemberPolicyRecommendation> recommendations =
                recommendationRepository.findByMember_IdOrderByScoreDescGeneratedAtDesc(member.getId());
        assertThat(recommendations)
                .extracting(recommendation -> recommendation.getPolicy().getTitle())
                .containsExactly("중소기업 재직청년 복지지원");
        assertThat(recommendations.get(0).getMatchReason()).contains("재직자 대상 조건 일치");
    }

    @Test
    void projectionBasedGenderClassificationExcludesFemaleOnlyPolicyForMaleMember() {
        Member member = persistEmployedYangjuMember();
        Policy femaleOnly = persistPolicy("GENDER-FLOW-1", "경력보유여성 면접 정장 대여 지원", gyeonggi);
        persistProjection(
                femaleOnly,
                "경력단절 및 경력보유 여성 구직자",
                "면접 예정인 여성 구직자",
                "면접 정장을 대여합니다."
        );
        Policy allGender = persistPolicy("GENDER-FLOW-2", "청년 문화생활 지원", gyeonggi);
        persistProjection(
                allGender,
                "만 19~34세 청년 누구나",
                "관내 청년",
                "문화생활비를 지원합니다."
        );
        entityManager.flush();

        PolicyGenderAudienceClassifier.GenderClassificationRebuildResult genderResult =
                genderAudienceClassifier.classifyMissingOrStale();
        entityManager.flush();
        entityManager.clear();

        service.refreshForMember(member.getId());

        List<MemberPolicyRecommendation> recommendations =
                recommendationRepository.findByMember_IdOrderByScoreDescGeneratedAtDesc(member.getId());
        assertThat(genderResult.processed()).isEqualTo(2);
        assertThat(recommendations)
                .extracting(recommendation -> recommendation.getPolicy().getTitle())
                .containsExactly("청년 문화생활 지원");
        assertThat(recommendations.get(0).getMatchReason()).contains("성별 제한 없음");
    }

    @Test
    void listHidesPreviouslyStoredFemaleOnlyRecommendationForMaleMember() {
        Member member = persistEmployedYangjuMember();
        Policy femaleOnly = persistPolicy("GENDER-FLOW-3", "경력보유여성 면접 정장 대여 지원", gyeonggi);
        persistProjection(
                femaleOnly,
                "경력단절 및 경력보유 여성 구직자",
                "면접 예정인 여성 구직자",
                "면접 정장을 대여합니다."
        );
        Policy allGender = persistPolicy("GENDER-FLOW-4", "청년 문화생활 지원", gyeonggi);
        persistProjection(
                allGender,
                "만 19~34세 청년 누구나",
                "관내 청년",
                "문화생활비를 지원합니다."
        );
        recommendationRepository.save(MemberPolicyRecommendation.create(
                member,
                femaleOnly,
                90,
                "경기도 거주 조건 일치 · 만 26세 연령 조건 일치",
                LocalDateTime.of(2026, 7, 25, 10, 0)
        ));
        recommendationRepository.save(MemberPolicyRecommendation.create(
                member,
                allGender,
                80,
                "경기도 거주 조건 일치 · 만 26세 연령 조건 일치",
                LocalDateTime.of(2026, 7, 25, 10, 0)
        ));
        entityManager.flush();

        genderAudienceClassifier.classifyMissingOrStale();
        entityManager.flush();
        entityManager.clear();

        PolicyRecommendationListResponse response = service.list(member.getId());

        assertThat(response.items())
                .extracting(item -> item.title())
                .containsExactly("청년 문화생활 지원");
    }

    @Test
    void sourceSpecificRegionEvidencePreventsStoredSidoFromBeingRecommendedAsWholeProvince() {
        Member member = persistEmployedYangjuMember();
        Policy storedAsGyeonggi = persistPolicy("REGION-FLOW-1", "2026년 평택시 청년창업 금융지원사업 이차보전 지원", gyeonggi);
        persistProjection(storedAsGyeonggi, "경기도 청년", "", "청년 창업 금융지원");
        entityManager.persist(new PolicyRegionClassification(
                storedAsGyeonggi,
                "CITY",
                BigDecimal.valueOf(0.9000),
                "{\"evidence\":[]}",
                "test",
                false
        ));
        Policy yangjuPolicy = persistPolicy("REGION-FLOW-2", "양주시 청년 지원사업", yangju);
        persistProjection(yangjuPolicy, "양주시 청년", "", "청년 지원");
        entityManager.flush();
        entityManager.clear();

        service.refreshForMember(member.getId());

        List<MemberPolicyRecommendation> recommendations =
                recommendationRepository.findByMember_IdOrderByScoreDescGeneratedAtDesc(member.getId());
        assertThat(recommendations)
                .extracting(recommendation -> recommendation.getPolicy().getTitle())
                .containsExactly("양주시 청년 지원사업");
    }

    @Test
    void titleSpecificRegionMentionPreventsStoredSidoFromBeingRecommendedBeforeClassificationRebuild() {
        Member member = persistEmployedYangjuMember();
        Policy storedAsGyeonggi = persistPolicy("REGION-FLOW-3", "2026년 평택시 청년창업 금융지원사업 이차보전 지원", gyeonggi);
        persistProjection(storedAsGyeonggi, "경기도 청년", "", "청년 창업 금융지원");
        Policy yangjuPolicy = persistPolicy("REGION-FLOW-4", "양주시 청년 지원사업", yangju);
        persistProjection(yangjuPolicy, "양주시 청년", "", "청년 지원");
        entityManager.flush();
        entityManager.clear();

        service.refreshForMember(member.getId());

        List<MemberPolicyRecommendation> recommendations =
                recommendationRepository.findByMember_IdOrderByScoreDescGeneratedAtDesc(member.getId());
        assertThat(recommendations)
                .extracting(recommendation -> recommendation.getPolicy().getTitle())
                .containsExactly("양주시 청년 지원사업");
    }

    @Test
    void listHidesPreviouslyStoredRecommendationsThatNoLongerPassStrictEligibility() {
        Member member = persistEmployedYangjuMember();
        Policy interview = persistPolicy("LIST-FLOW-1", "경기도 청년 면접수당", gyeonggi);
        persistProjection(interview, "도내 미취업 청년 대상", "구직 활동 중인 미취업 청년", "면접비 지원");
        Policy storedAsGyeonggi = persistPolicy("LIST-FLOW-2", "2026년 평택시 청년창업 금융지원사업 이차보전 지원", gyeonggi);
        persistProjection(storedAsGyeonggi, "경기도 청년", "", "청년 창업 금융지원");
        Policy jobMatching = persistPolicy("LIST-FLOW-4", "경기청년 일자리 매치업 플러스", gyeonggi);
        persistProjection(jobMatching, "경기도 청년", "",
                "취업 청년과 중소기업 간 일자리를 매칭하고 정규직으로 채용될 수 있도록 지원");
        Policy yangjuPolicy = persistPolicy("LIST-FLOW-3", "양주시 재직 청년 지원", yangju);
        persistProjection(yangjuPolicy, "현재 중소기업에 재직 중인 청년", "", "청년 복지 지원");
        recommendationRepository.save(MemberPolicyRecommendation.create(
                member,
                interview,
                70,
                "경기도 거주 조건 일치 · 만 26세 연령 조건 일치 · 현재 신청 가능",
                LocalDateTime.of(2026, 7, 25, 10, 0)
        ));
        recommendationRepository.save(MemberPolicyRecommendation.create(
                member,
                storedAsGyeonggi,
                70,
                "경기도 거주 조건 일치 · 만 26세 연령 조건 일치 · 현재 신청 가능",
                LocalDateTime.of(2026, 7, 25, 10, 0)
        ));
        recommendationRepository.save(MemberPolicyRecommendation.create(
                member,
                yangjuPolicy,
                95,
                "양주시 거주 조건 일치 · 재직자 대상 조건 일치",
                LocalDateTime.of(2026, 7, 25, 10, 0)
        ));
        recommendationRepository.save(MemberPolicyRecommendation.create(
                member,
                jobMatching,
                70,
                "경기도 거주 조건 일치 · 만 26세 연령 조건 일치 · 현재 신청 가능",
                LocalDateTime.of(2026, 7, 25, 10, 0)
        ));
        entityManager.flush();
        entityManager.clear();

        PolicyRecommendationListResponse response = service.list(member.getId());

        assertThat(response.items())
                .extracting(item -> item.title())
                .containsExactly("양주시 재직 청년 지원");
    }

    private Member persistEmployedYangjuMember() {
        Member member = Member.signUp(
                "yangju-" + System.nanoTime() + "@example.com",
                "password",
                "양주회원",
                Gender.MALE,
                LocalDate.of(2000, 3, 12),
                LocalDateTime.of(2026, 7, 25, 0, 0)
        );
        memberRepository.save(member);
        profileRepository.save(PolicyRecommendationProfile.create(
                member,
                "경기도",
                "양주시",
                UserEmploymentStatus.EMPLOYED,
                LocalDateTime.of(2026, 7, 25, 0, 0)
        ));
        return member;
    }

    private RegionCode persistRegion(RegionCode parent, String code, String province, String city, String level) {
        RegionCode region = new RegionCode(parent, code, province, city, level);
        entityManager.persist(region);
        return region;
    }

    private Policy persistPolicy(String sourcePolicyId, String title, RegionCode region) {
        Policy policy = new Policy(sourcePolicyId);
        policy.updateBasic(title, "기관", PolicyCategory.일자리, "요약", "https://example.com/" + sourcePolicyId,
                LocalDate.of(2026, 7, 1), LocalDate.of(2026, 8, 31), false, true, "신청중");
        policy.updateCondition(new PolicyCondition(19, 39, null, null, null, "만 19~39세 청년", false));
        policy.getRegions().add(new PolicyRegion(policy, region));
        policyRepository.save(policy);
        return policy;
    }

    private void persistProjection(Policy policy, String targetText, String qualificationText, String descriptionText) {
        PolicySearchProjection projection = new PolicySearchProjection(policy);
        projection.update(policy.getTitle(), policy.getTitle(), "", policy.getCategory().name(),
                descriptionText, "", targetText, qualificationText, "", policy.getAgencyName(),
                policy.getTitle() + " " + targetText + " " + qualificationText + " " + descriptionText,
                "test", false);
        entityManager.persist(projection);
    }
}
