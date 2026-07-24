package com.weaone.themoa.domain.policy.recommendation.service;

import com.weaone.themoa.common.exception.BusinessException;
import com.weaone.themoa.common.exception.ErrorCode;
import com.weaone.themoa.domain.member.entity.Gender;
import com.weaone.themoa.domain.member.entity.Member;
import com.weaone.themoa.domain.member.repository.MemberRepository;
import com.weaone.themoa.domain.policy.policy.region.ResolvedUserRegion;
import com.weaone.themoa.domain.policy.rag.dto.UserEmploymentStatus;
import com.weaone.themoa.domain.policy.recommendation.dto.request.PolicyRecommendationProfileCreateRequest;
import com.weaone.themoa.domain.policy.recommendation.dto.response.PolicyRecommendationProfileResponse;
import com.weaone.themoa.domain.policy.recommendation.entity.PolicyRecommendationProfile;
import com.weaone.themoa.domain.policy.recommendation.repository.PolicyRecommendationProfileRepository;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

class PolicyRecommendationProfileServiceTest {
    private final PolicyRecommendationProfileRepository profileRepository = mock(PolicyRecommendationProfileRepository.class);
    private final MemberRepository memberRepository = mock(MemberRepository.class);
    private final PolicyRecommendationAgeCalculator ageCalculator = mock(PolicyRecommendationAgeCalculator.class);
    private final PolicyRecommendationRegionService regionService = mock(PolicyRecommendationRegionService.class);
    private final PolicyRecommendationProfileService service =
            new PolicyRecommendationProfileService(profileRepository, memberRepository, ageCalculator, regionService);

    @Test
    void getReturnsConfiguredFalseWhenProfileMissing() {
        Member member = member(7L);
        given(memberRepository.findById(7L)).willReturn(Optional.of(member));
        given(ageCalculator.currentAge(member)).willReturn(27);
        given(profileRepository.findByMember_Id(7L)).willReturn(Optional.empty());

        PolicyRecommendationProfileResponse response = service.get(7L);

        assertThat(response.configured()).isFalse();
        assertThat(response.age()).isEqualTo(27);
        assertThat(response.residenceSido()).isNull();
    }

    @Test
    void createTrimsAndStoresValidatedProfile() {
        Member member = member(7L);
        given(memberRepository.findById(7L)).willReturn(Optional.of(member));
        given(ageCalculator.currentAge(member)).willReturn(27);
        given(profileRepository.existsByMember_Id(7L)).willReturn(false);
        given(regionService.validate(" 경기도 ", " 수원시 ")).willReturn(new PolicyRecommendationRegionService.ValidatedRegion(
                "경기도",
                "수원시",
                new ResolvedUserRegion("경기도", "수원시", null)
        ));
        given(profileRepository.save(any(PolicyRecommendationProfile.class))).willAnswer(invocation -> invocation.getArgument(0));

        PolicyRecommendationProfileResponse response = service.create(
                7L,
                new PolicyRecommendationProfileCreateRequest(" 경기도 ", " 수원시 ", UserEmploymentStatus.UNEMPLOYED)
        );

        assertThat(response.configured()).isTrue();
        assertThat(response.residenceSido()).isEqualTo("경기도");
        assertThat(response.residenceSigungu()).isEqualTo("수원시");
    }

    @Test
    void createRejectsDuplicateProfile() {
        Member member = member(7L);
        given(memberRepository.findById(7L)).willReturn(Optional.of(member));
        given(ageCalculator.currentAge(member)).willReturn(27);
        given(profileRepository.existsByMember_Id(7L)).willReturn(true);

        assertThatThrownBy(() -> service.create(
                7L,
                new PolicyRecommendationProfileCreateRequest("경기도", "수원시", UserEmploymentStatus.UNEMPLOYED)
        )).isInstanceOfSatisfying(BusinessException.class,
                ex -> assertThat(ex.getErrorCode())
                        .isEqualTo(ErrorCode.POLICY_RECOMMENDATION_PROFILE_ALREADY_EXISTS));
    }

    @Test
    void rejectsUnsupportedEmploymentStatus() {
        Member member = member(7L);
        given(memberRepository.findById(7L)).willReturn(Optional.of(member));
        given(ageCalculator.currentAge(member)).willReturn(27);
        given(profileRepository.existsByMember_Id(7L)).willReturn(false);

        assertThatThrownBy(() -> service.create(
                7L,
                new PolicyRecommendationProfileCreateRequest("경기도", "수원시", UserEmploymentStatus.UNKNOWN)
        )).isInstanceOfSatisfying(BusinessException.class,
                ex -> assertThat(ex.getErrorCode())
                        .isEqualTo(ErrorCode.POLICY_RECOMMENDATION_EMPLOYMENT_INVALID));
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
