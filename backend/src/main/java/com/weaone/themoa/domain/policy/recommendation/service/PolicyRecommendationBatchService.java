package com.weaone.themoa.domain.policy.recommendation.service;

import com.weaone.themoa.domain.policy.recommendation.entity.PolicyRecommendationProfile;
import com.weaone.themoa.domain.policy.recommendation.repository.PolicyRecommendationProfileRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

@Service
public class PolicyRecommendationBatchService {
    private static final Logger log = LoggerFactory.getLogger(PolicyRecommendationBatchService.class);
    private static final int BATCH_SIZE = 100;

    private final PolicyRecommendationProfileRepository profileRepository;
    private final PolicyRecommendationService recommendationService;

    public PolicyRecommendationBatchService(PolicyRecommendationProfileRepository profileRepository,
                                            PolicyRecommendationService recommendationService) {
        this.profileRepository = profileRepository;
        this.recommendationService = recommendationService;
    }

    public void refreshAllProfiles() {
        long startedAt = System.currentTimeMillis();
        int page = 0;
        int targetCount = 0;
        int successCount = 0;
        int failedCount = 0;
        Page<PolicyRecommendationProfile> profiles;
        do {
            profiles = profileRepository.findAll(
                    PageRequest.of(page, BATCH_SIZE, Sort.by(Sort.Direction.ASC, "id"))
            );
            for (PolicyRecommendationProfile profile : profiles.getContent()) {
                targetCount++;
                try {
                    recommendationService.refreshForMember(profile.getMember().getId());
                    successCount++;
                } catch (RuntimeException ex) {
                    failedCount++;
                    log.warn("정책 추천 재계산에 실패했습니다. memberId={}, errorType={}",
                            profile.getMember().getId(), ex.getClass().getSimpleName());
                }
            }
            page++;
        } while (profiles.hasNext());
        log.info("정책 추천 Batch가 완료됐습니다. target={}, success={}, failed={}, elapsedMs={}",
                targetCount, successCount, failedCount, System.currentTimeMillis() - startedAt);
    }
}
