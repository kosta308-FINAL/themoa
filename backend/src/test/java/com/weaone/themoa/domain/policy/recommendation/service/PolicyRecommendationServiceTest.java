package com.weaone.themoa.domain.policy.recommendation.service;

import com.weaone.themoa.domain.member.entity.Gender;
import com.weaone.themoa.domain.member.entity.Member;
import com.weaone.themoa.domain.member.repository.MemberRepository;
import com.weaone.themoa.domain.policy.policy.repository.PolicyRepository;
import com.weaone.themoa.domain.policy.policy.service.RegionEligiblePolicyCandidateService;
import com.weaone.themoa.domain.policy.rag.service.PolicyEmploymentAudienceClassifier;
import com.weaone.themoa.domain.policy.recommendation.dto.response.PolicyRecommendationListResponse;
import com.weaone.themoa.domain.policy.recommendation.repository.MemberPolicyRecommendationRepository;
import com.weaone.themoa.domain.policy.recommendation.repository.PolicyRecommendationProfileRepository;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

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
}
