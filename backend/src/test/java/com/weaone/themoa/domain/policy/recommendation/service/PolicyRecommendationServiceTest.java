package com.weaone.themoa.domain.policy.recommendation.service;

import com.weaone.themoa.domain.member.entity.Gender;
import com.weaone.themoa.domain.member.entity.Member;
import com.weaone.themoa.domain.member.repository.MemberRepository;
import com.weaone.themoa.domain.policy.policy.entity.Policy;
import com.weaone.themoa.domain.policy.policy.entity.PolicyCategory;
import com.weaone.themoa.domain.policy.policy.entity.RegionCode;
import com.weaone.themoa.domain.policy.policy.region.RegionCompatibility;
import com.weaone.themoa.domain.policy.policy.region.RegionEligiblePolicyCandidate;
import com.weaone.themoa.domain.policy.policy.region.ResolvedUserRegion;
import com.weaone.themoa.domain.policy.policy.region.SearchRegionLevel;
import com.weaone.themoa.domain.policy.policy.repository.PolicyRepository;
import com.weaone.themoa.domain.policy.policy.service.RegionEligiblePolicyCandidateService;
import com.weaone.themoa.domain.policy.rag.dto.PolicyEmploymentAudience;
import com.weaone.themoa.domain.policy.rag.dto.UserEmploymentStatus;
import com.weaone.themoa.domain.policy.rag.service.PolicyEmploymentAudienceClassifier;
import com.weaone.themoa.domain.policy.recommendation.dto.response.PolicyRecommendationListResponse;
import com.weaone.themoa.domain.policy.recommendation.entity.MemberPolicyRecommendation;
import com.weaone.themoa.domain.policy.recommendation.entity.PolicyRecommendationProfile;
import com.weaone.themoa.domain.policy.recommendation.repository.MemberPolicyRecommendationRepository;
import com.weaone.themoa.domain.policy.recommendation.repository.PolicyRecommendationProfileRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class PolicyRecommendationServiceTest {
    private final MemberRepository memberRepository = mock(MemberRepository.class);
    private final PolicyRecommendationProfileRepository profileRepository = mock(PolicyRecommendationProfileRepository.class);
    private final MemberPolicyRecommendationRepository recommendationRepository = mock(MemberPolicyRecommendationRepository.class);
    private final PolicyRepository policyRepository = mock(PolicyRepository.class);
    private final PolicyRecommendationAgeCalculator ageCalculator = mock(PolicyRecommendationAgeCalculator.class);
    private final PolicyRecommendationRegionService regionService = mock(PolicyRecommendationRegionService.class);
    private final RegionEligiblePolicyCandidateService regionCandidateService = mock(RegionEligiblePolicyCandidateService.class);
    private final PolicyEmploymentAudienceClassifier employmentAudienceClassifier = mock(PolicyEmploymentAudienceClassifier.class);
    private final PolicyRecommendationMatcher matcher = mock(PolicyRecommendationMatcher.class);
    private final PolicyRecommendationService service = new PolicyRecommendationService(
            memberRepository,
            profileRepository,
            recommendationRepository,
            policyRepository,
            ageCalculator,
            regionService,
            regionCandidateService,
            employmentAudienceClassifier,
            matcher
    );

    @Test
    void listReturnsConfiguredFalseWhenProfileMissing() {
        Member member = member(7L);
        given(memberRepository.findById(7L)).willReturn(Optional.of(member));
        given(ageCalculator.currentAge(member)).willReturn(27);
        given(profileRepository.findByMember_Id(7L)).willReturn(Optional.empty());

        PolicyRecommendationListResponse response = service.list(7L);

        assertThat(response.configured()).isFalse();
        assertThat(response.items()).isEmpty();
        assertThat(response.profile()).isNull();
    }

    @Test
    void refreshSortsByRegionPriorityBeforeScore() {
        Member member = member(7L);
        PolicyRecommendationProfile profile = profile(member);
        RegionCode suwon = region(10, "경기도", "수원시", "CITY");
        Policy nationwide = policy(1, "전국 정책");
        Policy suwonOnly = policy(2, "수원시 정책");
        Policy gyeonggi = policy(3, "경기도 정책");
        Policy multiSigungu = policy(4, "수원 포함 복수 지역 정책");

        givenRefreshBase(member, profile, suwon);
        given(regionCandidateService.findRecommendationEligibleCandidates(any()))
                .willReturn(List.of(
                        new RegionEligiblePolicyCandidate(nationwide.getId(), RegionCompatibility.NATIONWIDE),
                        new RegionEligiblePolicyCandidate(suwonOnly.getId(), RegionCompatibility.EXACT_SIGUNGU),
                        new RegionEligiblePolicyCandidate(gyeonggi.getId(), RegionCompatibility.PARENT_SIDO),
                        new RegionEligiblePolicyCandidate(multiSigungu.getId(), RegionCompatibility.MULTIPLE_SIGUNGU_MATCH)
                ));
        given(policyRepository.findWithRelationsByIdIn(any()))
                .willReturn(List.of(nationwide, suwonOnly, gyeonggi, multiSigungu));
        givenEmploymentAudiences(nationwide, suwonOnly, gyeonggi, multiSigungu);
        givenMatch(nationwide, RegionCompatibility.NATIONWIDE, 90);
        givenMatch(suwonOnly, RegionCompatibility.EXACT_SIGUNGU, 55);
        givenMatch(gyeonggi, RegionCompatibility.PARENT_SIDO, 70);
        givenMatch(multiSigungu, RegionCompatibility.MULTIPLE_SIGUNGU_MATCH, 60);

        service.refreshForMember(7L);

        List<MemberPolicyRecommendation> saved = savedRecommendations();
        assertThat(saved).extracting(recommendation -> recommendation.getPolicy().getId())
                .containsExactly(
                        suwonOnly.getId(),
                        multiSigungu.getId(),
                        gyeonggi.getId(),
                        nationwide.getId()
                );
    }

    @Test
    void refreshAppliesLimitAfterRegionPrioritySort() {
        Member member = member(7L);
        PolicyRecommendationProfile profile = profile(member);
        RegionCode suwon = region(10, "경기도", "수원시", "CITY");
        List<Policy> exactPolicies = java.util.stream.IntStream.rangeClosed(1, 25)
                .mapToObj(id -> policy(id, "수원시 정책 " + id))
                .toList();
        Policy nationwide = policy(100, "전국 고득점 정책");

        givenRefreshBase(member, profile, suwon);
        List<RegionEligiblePolicyCandidate> candidates = new java.util.ArrayList<>();
        exactPolicies.forEach(policy -> candidates.add(
                new RegionEligiblePolicyCandidate(policy.getId(), RegionCompatibility.EXACT_SIGUNGU)));
        candidates.add(new RegionEligiblePolicyCandidate(nationwide.getId(), RegionCompatibility.NATIONWIDE));
        given(regionCandidateService.findRecommendationEligibleCandidates(any())).willReturn(candidates);
        List<Policy> policies = new java.util.ArrayList<>(exactPolicies);
        policies.add(nationwide);
        given(policyRepository.findWithRelationsByIdIn(any())).willReturn(policies);
        givenEmploymentAudiences(policies.toArray(new Policy[0]));
        exactPolicies.forEach(policy -> givenMatch(policy, RegionCompatibility.EXACT_SIGUNGU, 50));
        givenMatch(nationwide, RegionCompatibility.NATIONWIDE, 100);

        service.refreshForMember(7L);

        List<MemberPolicyRecommendation> saved = savedRecommendations();
        assertThat(saved).hasSize(20);
        assertThat(saved).allMatch(recommendation -> recommendation.getPolicy().getId() <= 25);
    }

    private Member member(Long id) {
        Member member = Member.signUp(
                "user" + id + "@example.com",
                "password",
                "회원",
                Gender.MALE,
                LocalDate.of(1999, 3, 12),
                LocalDateTime.of(2026, 7, 24, 0, 0)
        );
        ReflectionTestUtils.setField(member, "id", id);
        return member;
    }

    private PolicyRecommendationProfile profile(Member member) {
        return PolicyRecommendationProfile.create(
                member,
                "경기도",
                "수원시",
                UserEmploymentStatus.UNEMPLOYED,
                LocalDateTime.of(2026, 7, 24, 0, 0)
        );
    }

    private Policy policy(Integer id, String title) {
        Policy policy = new Policy("YC-" + id);
        ReflectionTestUtils.setField(policy, "id", id);
        policy.updateBasic(
                title,
                "기관",
                PolicyCategory.일자리,
                "요약",
                "https://example.com/" + id,
                LocalDate.of(2026, 7, 1),
                LocalDate.of(2026, 8, 31),
                false,
                true,
                "신청중"
        );
        return policy;
    }

    private RegionCode region(Integer id, String province, String city, String level) {
        RegionCode region = new RegionCode(null, "TEST:" + id, province, city, level);
        ReflectionTestUtils.setField(region, "id", id);
        return region;
    }

    private void givenRefreshBase(Member member, PolicyRecommendationProfile profile, RegionCode userRegion) {
        given(memberRepository.findById(7L)).willReturn(Optional.of(member));
        given(ageCalculator.currentAge(member)).willReturn(27);
        given(profileRepository.findByMember_Id(7L)).willReturn(Optional.of(profile));
        given(regionService.validate("경기도", "수원시"))
                .willReturn(new PolicyRecommendationRegionService.ValidatedRegion(
                        "경기도",
                        "수원시",
                        new ResolvedUserRegion("경기도", "수원시", null, SearchRegionLevel.SIGUNGU, userRegion)
                ));
    }

    private void givenEmploymentAudiences(Policy... policies) {
        Map<Integer, PolicyEmploymentAudience> audiences = new LinkedHashMap<>();
        for (Policy policy : policies) {
            audiences.put(policy.getId(), PolicyEmploymentAudience.unknown());
        }
        given(employmentAudienceClassifier.classify(org.mockito.ArgumentMatchers.<Collection<Integer>>any()))
                .willReturn(audiences);
    }

    private void givenMatch(Policy policy, RegionCompatibility compatibility, int score) {
        given(matcher.match(
                eq(policy),
                eq(compatibility),
                eq(27),
                eq(UserEmploymentStatus.UNEMPLOYED),
                eq(PolicyEmploymentAudience.unknown()),
                any(LocalDate.class)
        )).willReturn(new PolicyRecommendationMatch(true, score, compatibility, compatibility.label()));
    }

    @SuppressWarnings("unchecked")
    private List<MemberPolicyRecommendation> savedRecommendations() {
        ArgumentCaptor<List<MemberPolicyRecommendation>> captor = ArgumentCaptor.forClass(List.class);
        verify(recommendationRepository).saveAll(captor.capture());
        return captor.getValue();
    }
}
