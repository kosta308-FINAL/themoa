package com.weaone.themoa.domain.policy.recommendation.service;

import com.weaone.themoa.common.exception.BusinessException;
import com.weaone.themoa.common.exception.ErrorCode;
import com.weaone.themoa.domain.member.entity.Member;
import com.weaone.themoa.domain.member.repository.MemberRepository;
import com.weaone.themoa.domain.policy.policy.entity.Policy;
import com.weaone.themoa.domain.policy.policy.entity.PolicyCondition;
import com.weaone.themoa.domain.policy.policy.entity.PolicyRegion;
import com.weaone.themoa.domain.policy.policy.entity.PolicyRegionClassification;
import com.weaone.themoa.domain.policy.policy.entity.RegionCode;
import com.weaone.themoa.domain.policy.policy.region.RegionCompatibility;
import com.weaone.themoa.domain.policy.policy.region.RegionEligiblePolicyCandidate;
import com.weaone.themoa.domain.policy.policy.region.RegionMatchEvaluator;
import com.weaone.themoa.domain.policy.policy.region.StrictPolicyRegionMentionExtractor;
import com.weaone.themoa.domain.policy.policy.repository.PolicyRepository;
import com.weaone.themoa.domain.policy.policy.repository.PolicyRegionClassificationRepository;
import com.weaone.themoa.domain.policy.policy.service.RegionEligiblePolicyCandidateService;
import com.weaone.themoa.domain.policy.rag.dto.PolicyEmploymentAudience;
import com.weaone.themoa.domain.policy.rag.dto.PolicyTargetAudienceClassification;
import com.weaone.themoa.domain.policy.rag.service.PolicyEmploymentAudienceClassifier;
import com.weaone.themoa.domain.policy.rag.service.PolicyTargetAudienceClassifier;
import com.weaone.themoa.domain.policy.recommendation.dto.response.PolicyRecommendationItemResponse;
import com.weaone.themoa.domain.policy.recommendation.dto.response.PolicyRecommendationListResponse;
import com.weaone.themoa.domain.policy.recommendation.dto.response.PolicyRecommendationProfileSummaryResponse;
import com.weaone.themoa.domain.policy.recommendation.entity.MemberPolicyRecommendation;
import com.weaone.themoa.domain.policy.recommendation.entity.PolicyRecommendationProfile;
import com.weaone.themoa.domain.policy.recommendation.repository.MemberPolicyRecommendationRepository;
import com.weaone.themoa.domain.policy.recommendation.repository.PolicyRecommendationProfileRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Comparator;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class PolicyRecommendationService {
    private static final Logger log = LoggerFactory.getLogger(PolicyRecommendationService.class);
    private static final ZoneId SEOUL_ZONE = ZoneId.of("Asia/Seoul");
    private static final int MAX_RECOMMENDATIONS = 20;

    private final MemberRepository memberRepository;
    private final PolicyRecommendationProfileRepository profileRepository;
    private final MemberPolicyRecommendationRepository recommendationRepository;
    private final PolicyRepository policyRepository;
    private final PolicyRecommendationAgeCalculator ageCalculator;
    private final PolicyRecommendationRegionService regionService;
    private final RegionEligiblePolicyCandidateService regionCandidateService;
    private final PolicyEmploymentAudienceClassifier employmentAudienceClassifier;
    private final PolicyTargetAudienceClassifier targetAudienceClassifier;
    private final PolicyRecommendationMatcher matcher;
    private final PolicyRegionClassificationRepository regionClassificationRepository;
    private final StrictPolicyRegionMentionExtractor regionMentionExtractor;
    private final RegionMatchEvaluator regionMatchEvaluator;

    public PolicyRecommendationService(MemberRepository memberRepository,
                                       PolicyRecommendationProfileRepository profileRepository,
                                       MemberPolicyRecommendationRepository recommendationRepository,
                                       PolicyRepository policyRepository,
                                       PolicyRecommendationAgeCalculator ageCalculator,
                                       PolicyRecommendationRegionService regionService,
                                       RegionEligiblePolicyCandidateService regionCandidateService,
                                       PolicyEmploymentAudienceClassifier employmentAudienceClassifier,
                                       PolicyTargetAudienceClassifier targetAudienceClassifier,
                                       PolicyRecommendationMatcher matcher,
                                       PolicyRegionClassificationRepository regionClassificationRepository,
                                       StrictPolicyRegionMentionExtractor regionMentionExtractor,
                                       RegionMatchEvaluator regionMatchEvaluator) {
        this.memberRepository = memberRepository;
        this.profileRepository = profileRepository;
        this.recommendationRepository = recommendationRepository;
        this.policyRepository = policyRepository;
        this.ageCalculator = ageCalculator;
        this.regionService = regionService;
        this.regionCandidateService = regionCandidateService;
        this.employmentAudienceClassifier = employmentAudienceClassifier;
        this.targetAudienceClassifier = targetAudienceClassifier;
        this.matcher = matcher;
        this.regionClassificationRepository = regionClassificationRepository;
        this.regionMentionExtractor = regionMentionExtractor;
        this.regionMatchEvaluator = regionMatchEvaluator;
    }

    @Transactional(readOnly = true)
    public PolicyRecommendationListResponse list(Long memberId) {
        Member member = member(memberId);
        int age = ageCalculator.currentAge(member);
        PolicyRecommendationProfile profile = profileRepository.findByMember_Id(memberId).orElse(null);
        if (profile == null) {
            return new PolicyRecommendationListResponse(false, null, null, List.of());
        }
        List<MemberPolicyRecommendation> recommendations =
                recommendationRepository.findByMember_IdOrderByScoreDescGeneratedAtDesc(memberId);
        LocalDateTime generatedAt = recommendations.stream()
                .map(MemberPolicyRecommendation::getGeneratedAt)
                .findFirst()
                .orElse(null);
        List<MemberPolicyRecommendation> eligibleRecommendations = filterStoredRecommendations(recommendations, profile, age);
        return new PolicyRecommendationListResponse(
                true,
                generatedAt,
                summary(member, age, profile),
                eligibleRecommendations.stream().map(this::item).toList()
        );
    }

    private List<MemberPolicyRecommendation> filterStoredRecommendations(List<MemberPolicyRecommendation> recommendations,
                                                                         PolicyRecommendationProfile profile,
                                                                         int age) {
        if (recommendations.isEmpty()) {
            return List.of();
        }
        PolicyRecommendationRegionService.ValidatedRegion region =
                regionService.validate(profile.getResidenceSido(), profile.getResidenceSigungu());
        List<Policy> policies = recommendations.stream().map(MemberPolicyRecommendation::getPolicy).toList();
        Map<Integer, PolicyEmploymentAudience> employmentAudiences = classifyEmployment(policies);
        Map<Integer, PolicyTargetAudienceClassification> targetAudiences = classifyTargetAudience(policies);
        Map<Integer, PolicyRegionClassification> regionClassifications = findRegionClassifications(policies);
        LocalDate today = LocalDate.now(SEOUL_ZONE);
        return recommendations.stream()
                .filter(recommendation -> storedRecommendationStillEligible(recommendation, profile, age, region,
                        employmentAudiences, targetAudiences, regionClassifications, today))
                .toList();
    }

    private boolean storedRecommendationStillEligible(MemberPolicyRecommendation recommendation,
                                                      PolicyRecommendationProfile profile,
                                                      int age,
                                                      PolicyRecommendationRegionService.ValidatedRegion region,
                                                      Map<Integer, PolicyEmploymentAudience> employmentAudiences,
                                                      Map<Integer, PolicyTargetAudienceClassification> targetAudiences,
                                                      Map<Integer, PolicyRegionClassification> regionClassifications,
                                                      LocalDate today) {
        Policy policy = recommendation.getPolicy();
        RegionCompatibility compatibility = regionMatchEvaluator.evaluate(policy, region.resolvedUserRegion()).compatibility();
        boolean regionClassificationConflict = regionClassificationConflict(policy, regionClassifications.get(policy.getId()));
        PolicyRecommendationMatch match = matcher.match(
                policy,
                compatibility,
                age,
                profile.getEmploymentStatus(),
                employmentAudiences.get(policy.getId()),
                targetAudiences.get(policy.getId()),
                today,
                regionClassificationConflict
        );
        if (!match.matched()) {
            logRecommendationDecision(policy, compatibility, employmentAudiences.get(policy.getId()),
                    targetAudiences.get(policy.getId()), match, false);
        }
        return match.matched();
    }

    @Transactional
    public PolicyRecommendationListResponse refreshForMember(Long memberId) {
        Member member = member(memberId);
        int age = ageCalculator.currentAge(member);
        PolicyRecommendationProfile profile = profileRepository.findByMember_Id(memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.POLICY_RECOMMENDATION_PROFILE_NOT_FOUND));
        List<MemberPolicyRecommendation> next = calculate(member, profile, age);
        recommendationRepository.deleteAllByMemberId(memberId);
        recommendationRepository.saveAll(next);
        return new PolicyRecommendationListResponse(
                true,
                next.stream().map(MemberPolicyRecommendation::getGeneratedAt).findFirst().orElse(null),
                summary(member, age, profile),
                next.stream().map(this::item).toList()
        );
    }

    private List<MemberPolicyRecommendation> calculate(Member member, PolicyRecommendationProfile profile, int age) {
        LocalDate today = LocalDate.now(SEOUL_ZONE);
        LocalDateTime generatedAt = LocalDateTime.now(SEOUL_ZONE);
        PolicyRecommendationRegionService.ValidatedRegion region =
                regionService.validate(profile.getResidenceSido(), profile.getResidenceSigungu());
        List<RegionEligiblePolicyCandidate> candidates =
                regionCandidateService.findRecommendationEligibleCandidates(region.resolvedUserRegion());
        if (candidates.isEmpty()) {
            return List.of();
        }
        Map<Integer, RegionCompatibility> compatibilityByPolicyId = new LinkedHashMap<>();
        candidates.forEach(candidate -> compatibilityByPolicyId.put(candidate.policyId(), candidate.compatibility()));
        List<Policy> policies = policyRepository.findWithRelationsByIdIn(compatibilityByPolicyId.keySet().stream().toList());
        Map<Integer, PolicyEmploymentAudience> employmentAudiences =
                employmentAudienceClassifier.classify(compatibilityByPolicyId.keySet());
        Map<Integer, PolicyTargetAudienceClassification> targetAudiences =
                targetAudienceClassifier.classify(compatibilityByPolicyId.keySet());
        Map<Integer, PolicyRegionClassification> regionClassifications =
                findRegionClassifications(policies);
        return policies.stream()
                .map(policy -> scored(member, policy, compatibilityByPolicyId.get(policy.getId()), age, profile,
                        employmentAudiences.get(policy.getId()), targetAudiences.get(policy.getId()),
                        regionClassifications.get(policy.getId()), today, generatedAt))
                .filter(scored -> scored != null)
                .sorted(Comparator
                        .comparingInt(ScoredRecommendation::regionPriority)
                        .thenComparing(Comparator.comparingInt(ScoredRecommendation::score).reversed())
                        .thenComparing(scored -> dueDateForSort(scored.policy()))
                        .thenComparing(scored -> scored.policy().getId()))
                .limit(MAX_RECOMMENDATIONS)
                .map(ScoredRecommendation::recommendation)
                .toList();
    }

    private ScoredRecommendation scored(Member member, Policy policy, RegionCompatibility compatibility, int age,
                                        PolicyRecommendationProfile profile, PolicyEmploymentAudience audience,
                                        PolicyTargetAudienceClassification targetAudience,
                                        PolicyRegionClassification regionClassification,
                                        LocalDate today, LocalDateTime generatedAt) {
        boolean regionClassificationConflict = regionClassificationConflict(policy, regionClassification);
        PolicyRecommendationMatch match = matcher.match(policy, compatibility, age, profile.getEmploymentStatus(),
                audience, targetAudience, today, regionClassificationConflict);
        if (!match.matched()) {
            logRecommendationDecision(policy, compatibility, audience, targetAudience, match, false);
            return null;
        }
        MemberPolicyRecommendation recommendation = MemberPolicyRecommendation.create(
                member,
                policy,
                match.score(),
                match.matchReason(),
                generatedAt
        );
        logRecommendationDecision(policy, compatibility, audience, targetAudience, match, true);
        return new ScoredRecommendation(policy, match.score(), match.regionCompatibility(), recommendation);
    }

    private Map<Integer, PolicyRegionClassification> findRegionClassifications(List<Policy> policies) {
        if (policies.isEmpty()) {
            return Map.of();
        }
        Map<Integer, PolicyRegionClassification> classifications = new LinkedHashMap<>();
        regionClassificationRepository.findAllById(policies.stream().map(Policy::getId).toList())
                .forEach(classification -> classifications.put(classification.getPolicyId(), classification));
        return classifications;
    }

    private Map<Integer, PolicyEmploymentAudience> classifyEmployment(List<Policy> policies) {
        return employmentAudienceClassifier.classify(policyIds(policies));
    }

    private Map<Integer, PolicyTargetAudienceClassification> classifyTargetAudience(List<Policy> policies) {
        return targetAudienceClassifier.classify(policyIds(policies));
    }

    private Collection<Integer> policyIds(List<Policy> policies) {
        return policies.stream().map(Policy::getId).toList();
    }

    private boolean regionClassificationConflict(Policy policy, PolicyRegionClassification classification) {
        Set<String> storedSpecificRegions = policy.getRegions().stream()
                .map(PolicyRegion::getRegion)
                .filter(this::isSpecificRegion)
                .map(this::regionKey)
                .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));
        Set<RegionCode> extractedTitleRegions = regionMentionExtractor.extractFromTitle(policy.getTitle());
        Set<String> titleSpecificMentions = (extractedTitleRegions == null ? Set.<RegionCode>of() : extractedTitleRegions).stream()
                .filter(this::isSpecificRegion)
                .map(this::regionKey)
                .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));
        if (!titleSpecificMentions.isEmpty() && !storedSpecificRegions.containsAll(titleSpecificMentions)) {
            return true;
        }
        return classification != null
                && isSpecificClassification(classification)
                && storedSpecificRegions.isEmpty();
    }

    private boolean isSpecificRegion(RegionCode region) {
        return region != null && region.getCity() != null && !region.getCity().isBlank();
    }

    private String regionKey(RegionCode region) {
        return region.getProvince() + "|" + region.getCity();
    }

    private boolean isSpecificClassification(PolicyRegionClassification classification) {
        return "CITY".equals(classification.getRegionScope()) || "DISTRICT".equals(classification.getRegionScope());
    }

    private void logRecommendationDecision(Policy policy, RegionCompatibility compatibility,
                                           PolicyEmploymentAudience audience,
                                           PolicyTargetAudienceClassification targetAudience,
                                           PolicyRecommendationMatch match,
                                           boolean saved) {
        if (!log.isDebugEnabled()) {
            return;
        }
        log.debug("Policy recommendation decision. policyId={}, title={}, regions={}, compatibility={}, "
                        + "employmentAllowed={}, employmentEvidence={}, employmentExclusive={}, employmentConflict={}, "
                        + "targetStages={}, targetEvidence={}, targetExclusive={}, exclusionReason={}, score={}, saved={}",
                policy.getId(),
                policy.getTitle(),
                policy.getRegions().stream().map(region -> region.getRegion().displayName()).toList(),
                compatibility,
                audience == null ? null : audience.allowedStatuses(),
                audience == null ? null : audience.evidence(),
                audience != null && audience.exclusive(),
                audience != null && audience.conflict(),
                targetAudience == null ? null : targetAudience.includedStages(),
                targetAudience == null ? null : targetAudience.evidence(),
                targetAudience != null && targetAudience.stageExclusive(),
                match.exclusionReason(),
                match.score(),
                saved);
    }

    private LocalDate dueDateForSort(Policy policy) {
        if (policy.isAlwaysOpen() || policy.getDueDate() == null) {
            return LocalDate.MAX;
        }
        return policy.getDueDate();
    }

    private Member member(Long memberId) {
        return memberRepository.findById(memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));
    }

    private PolicyRecommendationProfileSummaryResponse summary(Member member, int age, PolicyRecommendationProfile profile) {
        return new PolicyRecommendationProfileSummaryResponse(
                member.getBirthDate(),
                age,
                profile.getResidenceSido(),
                profile.getResidenceSigungu(),
                profile.getEmploymentStatus()
        );
    }

    private PolicyRecommendationItemResponse item(MemberPolicyRecommendation recommendation) {
        Policy policy = recommendation.getPolicy();
        PolicyCondition condition = policy.getCondition();
        return new PolicyRecommendationItemResponse(
                policy.getId(),
                policy.getSourcePolicyId(),
                policy.getTitle(),
                policy.getCategory().name(),
                regionText(policy),
                policy.getAgencyName(),
                policy.getSummary() == null ? "" : policy.getSummary(),
                condition == null ? null : condition.getMinAge(),
                condition == null ? null : condition.getMaxAge(),
                condition == null ? null : condition.getEmploymentStatus(),
                policy.getStartDate(),
                policy.getDueDate(),
                policy.isAlwaysOpen(),
                policy.getStatus(),
                policy.getOfficialUrl() == null ? "" : policy.getOfficialUrl(),
                recommendation.getScore(),
                recommendation.getMatchReason()
        );
    }

    private String regionText(Policy policy) {
        List<String> regions = policy.getRegions().stream()
                .map(PolicyRegion::getRegion)
                .map(RegionCode::displayName)
                .distinct()
                .toList();
        return regions.isEmpty() ? "지역 확인 필요" : String.join(", ", regions);
    }

    private record ScoredRecommendation(
            Policy policy,
            int score,
            RegionCompatibility regionCompatibility,
            MemberPolicyRecommendation recommendation
    ) {
        private int regionPriority() {
            return regionCompatibility == null ? RegionCompatibility.UNKNOWN.priority() : regionCompatibility.priority();
        }
    }
}
