package com.weaone.themoa.domain.policy.recommendation.service;

import com.weaone.themoa.common.exception.BusinessException;
import com.weaone.themoa.common.exception.ErrorCode;
import com.weaone.themoa.domain.member.entity.Member;
import com.weaone.themoa.domain.member.repository.MemberRepository;
import com.weaone.themoa.domain.policy.rag.dto.UserEmploymentStatus;
import com.weaone.themoa.domain.policy.recommendation.dto.request.PolicyRecommendationProfileCreateRequest;
import com.weaone.themoa.domain.policy.recommendation.dto.request.PolicyRecommendationProfileUpdateRequest;
import com.weaone.themoa.domain.policy.recommendation.dto.response.PolicyRecommendationProfileResponse;
import com.weaone.themoa.domain.policy.recommendation.entity.PolicyRecommendationProfile;
import com.weaone.themoa.domain.policy.recommendation.repository.PolicyRecommendationProfileRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;

@Service
public class PolicyRecommendationProfileService {
    private static final ZoneId SEOUL_ZONE = ZoneId.of("Asia/Seoul");

    private final PolicyRecommendationProfileRepository profileRepository;
    private final MemberRepository memberRepository;
    private final PolicyRecommendationAgeCalculator ageCalculator;
    private final PolicyRecommendationRegionService regionService;

    public PolicyRecommendationProfileService(PolicyRecommendationProfileRepository profileRepository,
                                              MemberRepository memberRepository,
                                              PolicyRecommendationAgeCalculator ageCalculator,
                                              PolicyRecommendationRegionService regionService) {
        this.profileRepository = profileRepository;
        this.memberRepository = memberRepository;
        this.ageCalculator = ageCalculator;
        this.regionService = regionService;
    }

    @Transactional(readOnly = true)
    public PolicyRecommendationProfileResponse get(Long memberId) {
        Member member = member(memberId);
        int age = ageCalculator.currentAge(member);
        return profileRepository.findByMember_Id(memberId)
                .map(profile -> response(member, age, profile))
                .orElseGet(() -> new PolicyRecommendationProfileResponse(
                        false,
                        member.getBirthDate(),
                        age,
                        null,
                        null,
                        null,
                        null
                ));
    }

    @Transactional
    public PolicyRecommendationProfileResponse create(Long memberId, PolicyRecommendationProfileCreateRequest request) {
        Member member = member(memberId);
        int age = ageCalculator.currentAge(member);
        if (profileRepository.existsByMember_Id(memberId)) {
            throw new BusinessException(ErrorCode.POLICY_RECOMMENDATION_PROFILE_ALREADY_EXISTS);
        }
        PolicyRecommendationRegionService.ValidatedRegion region =
                regionService.validate(request.residenceSido(), request.residenceSigungu());
        UserEmploymentStatus employmentStatus = validateEmploymentStatus(request.employmentStatus());
        LocalDateTime now = LocalDateTime.now(SEOUL_ZONE);
        PolicyRecommendationProfile profile = profileRepository.save(PolicyRecommendationProfile.create(
                member,
                region.residenceSido(),
                region.residenceSigungu(),
                employmentStatus,
                now
        ));
        return response(member, age, profile);
    }

    @Transactional
    public PolicyRecommendationProfileResponse update(Long memberId, PolicyRecommendationProfileUpdateRequest request) {
        Member member = member(memberId);
        int age = ageCalculator.currentAge(member);
        PolicyRecommendationProfile profile = profileRepository.findByMember_Id(memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.POLICY_RECOMMENDATION_PROFILE_NOT_FOUND));
        PolicyRecommendationRegionService.ValidatedRegion region =
                regionService.validate(request.residenceSido(), request.residenceSigungu());
        UserEmploymentStatus employmentStatus = validateEmploymentStatus(request.employmentStatus());
        profile.update(region.residenceSido(), region.residenceSigungu(), employmentStatus, LocalDateTime.now(SEOUL_ZONE));
        return response(member, age, profile);
    }

    private Member member(Long memberId) {
        return memberRepository.findById(memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));
    }

    private UserEmploymentStatus validateEmploymentStatus(UserEmploymentStatus employmentStatus) {
        if (employmentStatus != UserEmploymentStatus.EMPLOYED && employmentStatus != UserEmploymentStatus.UNEMPLOYED) {
            throw new BusinessException(ErrorCode.POLICY_RECOMMENDATION_EMPLOYMENT_INVALID);
        }
        return employmentStatus;
    }

    private PolicyRecommendationProfileResponse response(Member member, int age, PolicyRecommendationProfile profile) {
        return new PolicyRecommendationProfileResponse(
                true,
                member.getBirthDate(),
                age,
                profile.getResidenceSido(),
                profile.getResidenceSigungu(),
                profile.getEmploymentStatus(),
                profile.getUpdatedAt()
        );
    }
}
