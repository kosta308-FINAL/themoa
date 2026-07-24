package com.weaone.themoa.domain.policy.recommendation.service;

import com.weaone.themoa.common.exception.BusinessException;
import com.weaone.themoa.common.exception.ErrorCode;
import com.weaone.themoa.domain.member.entity.Member;
import com.weaone.themoa.domain.member.repository.MemberRepository;
import com.weaone.themoa.domain.policy.policy.entity.Policy;
import com.weaone.themoa.domain.policy.policy.entity.PolicyCondition;
import com.weaone.themoa.domain.policy.policy.entity.PolicyRegion;
import com.weaone.themoa.domain.policy.policy.entity.RegionCode;
import com.weaone.themoa.domain.policy.policy.region.RegionCompatibility;
import com.weaone.themoa.domain.policy.policy.region.RegionEligiblePolicyCandidate;
import com.weaone.themoa.domain.policy.policy.repository.PolicyRepository;
import com.weaone.themoa.domain.policy.policy.service.RegionEligiblePolicyCandidateService;
import com.weaone.themoa.domain.policy.rag.dto.PolicyEmploymentAudience;
import com.weaone.themoa.domain.policy.rag.service.PolicyEmploymentAudienceClassifier;
import com.weaone.themoa.domain.policy.recommendation.dto.response.PolicyRecommendationItemResponse;
import com.weaone.themoa.domain.policy.recommendation.dto.response.PolicyRecommendationListResponse;
import com.weaone.themoa.domain.policy.recommendation.dto.response.PolicyRecommendationProfileSummaryResponse;
import com.weaone.themoa.domain.policy.recommendation.entity.MemberPolicyRecommendation;
import com.weaone.themoa.domain.policy.recommendation.entity.PolicyRecommendationProfile;
import com.weaone.themoa.domain.policy.recommendation.repository.MemberPolicyRecommendationRepository;
import com.weaone.themoa.domain.policy.recommendation.repository.PolicyRecommendationProfileRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class PolicyRecommendationService {
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
    private final PolicyRecommendationMatcher matcher;

    public PolicyRecommendationService(MemberRepository memberRepository,
                                       PolicyRecommendationProfileRepository profileRepository,
                                       MemberPolicyRecommendationRepository recommendationRepository,
                                       PolicyRepository policyRepository,
                                       PolicyRecommendationAgeCalculator ageCalculator,
                                       PolicyRecommendationRegionService regionService,
                                       RegionEligiblePolicyCandidateService regionCandidateService,
                                       PolicyEmploymentAudienceClassifier employmentAudienceClassifier,
                                       PolicyRecommendationMatcher matcher) {
        this.memberRepository = memberRepository;
        this.profileRepository = profileRepository;
        this.recommendationRepository = recommendationRepository;
        this.policyRepository = policyRepository;
        this.ageCalculator = ageCalculator;
        this.regionService = regionService;
        this.regionCandidateService = regionCandidateService;
        this.employmentAudienceClassifier = employmentAudienceClassifier;
        this.matcher = matcher;
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
        return new PolicyRecommendationListResponse(
                true,
                generatedAt,
                summary(member, age, profile),
                recommendations.stream().map(this::item).toList()
        );
    }

    @Transactional
    public PolicyRecommendationListResponse refreshForMember(Long memberId) {
        Member member = member(memberId);
        int age = ageCalculator.currentAge(member);
        PolicyRecommendationProfile profile = profileRepository.findByMember_Id(memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.POLICY_RECOMMENDATION_PROFILE_NOT_FOUND));
        List<MemberPolicyRecommendation> next = calculate(member, profile, age);
        recommendationRepository.deleteByMember_Id(memberId);
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
        return policies.stream()
                .map(policy -> scored(member, policy, compatibilityByPolicyId.get(policy.getId()), age, profile,
                        employmentAudiences.get(policy.getId()), today, generatedAt))
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
                                        LocalDate today, LocalDateTime generatedAt) {
        PolicyRecommendationMatch match = matcher.match(policy, compatibility, age, profile.getEmploymentStatus(), audience, today);
        if (!match.matched()) {
            return null;
        }
        MemberPolicyRecommendation recommendation = MemberPolicyRecommendation.create(
                member,
                policy,
                match.score(),
                match.matchReason(),
                generatedAt
        );
        return new ScoredRecommendation(policy, match.score(), match.regionCompatibility(), recommendation);
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
        return regions.isEmpty() ? "전국" : String.join(", ", regions);
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
