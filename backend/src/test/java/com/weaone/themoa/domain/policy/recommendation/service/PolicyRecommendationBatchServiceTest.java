package com.weaone.themoa.domain.policy.recommendation.service;

import com.weaone.themoa.domain.member.entity.Gender;
import com.weaone.themoa.domain.member.entity.Member;
import com.weaone.themoa.domain.policy.rag.dto.UserEmploymentStatus;
import com.weaone.themoa.domain.policy.recommendation.entity.PolicyRecommendationProfile;
import com.weaone.themoa.domain.policy.recommendation.repository.PolicyRecommendationProfileRepository;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PolicyRecommendationBatchServiceTest {
    private static final PageRequest FIRST_PAGE =
            PageRequest.of(0, 100, Sort.by(Sort.Direction.ASC, "id"));

    private final PolicyRecommendationProfileRepository profileRepository = mock(PolicyRecommendationProfileRepository.class);
    private final PolicyRecommendationService recommendationService = mock(PolicyRecommendationService.class);
    private final PolicyRecommendationBatchService service =
            new PolicyRecommendationBatchService(profileRepository, recommendationService);

    @Test
    void refreshesOnlyConfiguredProfiles() {
        PolicyRecommendationProfile profile = profile(7L);
        when(profileRepository.findAll(FIRST_PAGE)).thenReturn(new PageImpl<>(List.of(profile)));

        service.refreshAllProfiles();

        verify(profileRepository).findAll(FIRST_PAGE);
        verify(recommendationService).refreshForMember(7L);
    }

    @Test
    void continuesAfterMemberFailure() {
        PolicyRecommendationProfile first = profile(7L);
        PolicyRecommendationProfile second = profile(8L);
        when(profileRepository.findAll(FIRST_PAGE)).thenReturn(new PageImpl<>(List.of(first, second)));
        when(recommendationService.refreshForMember(7L)).thenThrow(new IllegalStateException("failed"));

        service.refreshAllProfiles();

        verify(recommendationService).refreshForMember(8L);
    }

    private PolicyRecommendationProfile profile(Long memberId) {
        Member member = Member.signUp(
                "user" + memberId + "@example.com",
                "password",
                "회원",
                Gender.MALE,
                LocalDate.of(1999, 3, 12),
                LocalDateTime.of(2026, 7, 24, 0, 0)
        );
        ReflectionTestUtils.setField(member, "id", memberId);
        return PolicyRecommendationProfile.create(
                member,
                "경기도",
                "수원시",
                UserEmploymentStatus.UNEMPLOYED,
                LocalDateTime.of(2026, 7, 24, 0, 0)
        );
    }
}
