package com.weaone.themoa.domain.policy.bookmark.support;

import com.weaone.themoa.domain.member.repository.MemberRepository;
import com.weaone.themoa.domain.policy.bookmark.repository.PolicyBookmarkRepository;
import com.weaone.themoa.domain.policy.bookmark.service.PolicyBookmarkService;
import com.weaone.themoa.domain.policy.policy.entity.Policy;
import com.weaone.themoa.domain.policy.policy.repository.PolicyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@Order(30)
@ConditionalOnProperty(
        prefix = "app.demo-seed",
        name = "policy-bookmarks-enabled",
        havingValue = "true"
)
@RequiredArgsConstructor
public class DemoPolicyBookmarkSeeder implements ApplicationRunner {

    private static final Long DEMO_MEMBER_ID = 1L;
    private static final List<String> DEMO_POLICY_TITLES = List.of(
            "K-패스(K패스)",
            "청년 자산형성 지원(청년도약계좌)",
            "경기청년 일자리 매치업 플러스",
            "경기도 청년 면접수당",
            "경기청년 맞춤형 채용지원 서비스"
    );

    private final MemberRepository memberRepository;
    private final PolicyRepository policyRepository;
    private final PolicyBookmarkRepository policyBookmarkRepository;
    private final PolicyBookmarkService policyBookmarkService;

    @Override
    public void run(ApplicationArguments args) {
        log.info("Demo policy bookmark seed started for member id {}.", DEMO_MEMBER_ID);
        if (!memberRepository.existsById(DEMO_MEMBER_ID)) {
            log.warn("Demo policy bookmark seed skipped: member id {} does not exist.", DEMO_MEMBER_ID);
            return;
        }

        int createdCount = 0;
        int skippedCount = 0;
        int missingCount = 0;
        int failedCount = 0;

        for (String title : DEMO_POLICY_TITLES) {
            try {
                SeedResult result = seedPolicyBookmark(title);
                createdCount += result.createdCount();
                skippedCount += result.skippedCount();
                missingCount += result.missingCount();
            } catch (RuntimeException ex) {
                failedCount++;
                log.warn("Demo policy bookmark seed failed for title={}. message={}", title, ex.getMessage(), ex);
            }
        }

        log.info("Demo policy bookmark seed completed. created={}, skipped={}, missing={}, failed={}",
                createdCount, skippedCount, missingCount, failedCount);
    }

    private SeedResult seedPolicyBookmark(String title) {
        List<Policy> policies = policyRepository.findByTitleAndActiveTrue(title);
        if (policies.isEmpty()) {
            log.warn("Demo policy bookmark skipped: target policy not found. title={}", title);
            return SeedResult.missing();
        }
        if (policies.size() > 1) {
            log.warn("Demo policy bookmark skipped: duplicate active policies found. title={}, count={}",
                    title, policies.size());
            return SeedResult.skipped();
        }

        Policy policy = policies.get(0);
        Integer policyId = policy.getId();
        if (policyBookmarkRepository.existsByMember_IdAndPolicy_Id(DEMO_MEMBER_ID, policyId)) {
            log.info("Demo policy bookmark already exists. memberId={}, policyId={}", DEMO_MEMBER_ID, policyId);
            return SeedResult.skipped();
        }

        policyBookmarkService.add(DEMO_MEMBER_ID, policyId);
        log.info("Demo policy bookmark created. memberId={}, policyId={}", DEMO_MEMBER_ID, policyId);
        return SeedResult.created();
    }

    private record SeedResult(int createdCount, int skippedCount, int missingCount) {
        private static SeedResult created() {
            return new SeedResult(1, 0, 0);
        }

        private static SeedResult skipped() {
            return new SeedResult(0, 1, 0);
        }

        private static SeedResult missing() {
            return new SeedResult(0, 0, 1);
        }
    }
}
