package com.weaone.themoa.domain.policy.recommendation.service;

import com.weaone.themoa.domain.policy.policy.entity.Policy;
import com.weaone.themoa.domain.policy.policy.entity.PolicyCategory;
import com.weaone.themoa.domain.policy.policy.entity.PolicyCondition;
import com.weaone.themoa.domain.policy.policy.entity.PolicyRegion;
import com.weaone.themoa.domain.policy.policy.entity.RegionCode;
import com.weaone.themoa.domain.policy.policy.region.RegionCompatibility;
import com.weaone.themoa.domain.policy.rag.dto.PolicyEmploymentAudience;
import com.weaone.themoa.domain.policy.rag.dto.UserEmploymentStatus;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class PolicyRecommendationMatcherTest {
    private final PolicyRecommendationMatcher matcher = new PolicyRecommendationMatcher();

    @Test
    void exactSigunguAgeEmploymentAndOpenPolicyScores() {
        Policy policy = policy(1, 19, 34, LocalDate.of(2026, 7, 1), LocalDate.of(2026, 8, 31));
        PolicyEmploymentAudience audience =
                new PolicyEmploymentAudience(Set.of(UserEmploymentStatus.UNEMPLOYED), true, 0.9, List.of());

        PolicyRecommendationMatch match = matcher.match(
                policy,
                RegionCompatibility.EXACT_SIGUNGU,
                27,
                UserEmploymentStatus.UNEMPLOYED,
                audience,
                LocalDate.of(2026, 7, 24)
        );

        assertThat(match.matched()).isTrue();
        assertThat(match.score()).isEqualTo(105);
        assertThat(match.regionCompatibility()).isEqualTo(RegionCompatibility.EXACT_SIGUNGU);
        assertThat(match.matchReason()).contains("연령 조건 일치", "미취업 대상 조건 일치", "현재 신청 가능");
    }

    @Test
    void multipleSigunguMatchScoresHigherThanSidoAndNationwide() {
        Policy policy = policy(1, null, null, LocalDate.of(2026, 7, 1), LocalDate.of(2026, 8, 31));

        PolicyRecommendationMatch multipleSigungu = matcher.match(
                policy,
                RegionCompatibility.MULTIPLE_SIGUNGU_MATCH,
                27,
                UserEmploymentStatus.UNEMPLOYED,
                PolicyEmploymentAudience.unknown(),
                LocalDate.of(2026, 7, 24)
        );
        PolicyRecommendationMatch exactSido = matcher.match(
                policy,
                RegionCompatibility.EXACT_SIDO,
                27,
                UserEmploymentStatus.UNEMPLOYED,
                PolicyEmploymentAudience.unknown(),
                LocalDate.of(2026, 7, 24)
        );
        PolicyRecommendationMatch nationwide = matcher.match(
                policy,
                RegionCompatibility.NATIONWIDE,
                27,
                UserEmploymentStatus.UNEMPLOYED,
                PolicyEmploymentAudience.unknown(),
                LocalDate.of(2026, 7, 24)
        );

        assertThat(multipleSigungu.score()).isGreaterThan(exactSido.score());
        assertThat(exactSido.score()).isGreaterThan(nationwide.score());
        assertThat(multipleSigungu.matchReason()).contains("복수 대상 지역");
    }

    @Test
    void multipleSidoMatchScoresHigherThanNationwide() {
        Policy policy = policy(1, null, null, LocalDate.of(2026, 7, 1), LocalDate.of(2026, 8, 31));

        PolicyRecommendationMatch multipleSido = matcher.match(
                policy,
                RegionCompatibility.MULTIPLE_SIDO_MATCH,
                27,
                UserEmploymentStatus.UNEMPLOYED,
                PolicyEmploymentAudience.unknown(),
                LocalDate.of(2026, 7, 24)
        );
        PolicyRecommendationMatch nationwide = matcher.match(
                policy,
                RegionCompatibility.NATIONWIDE,
                27,
                UserEmploymentStatus.UNEMPLOYED,
                PolicyEmploymentAudience.unknown(),
                LocalDate.of(2026, 7, 24)
        );

        assertThat(multipleSido.score()).isGreaterThan(nationwide.score());
        assertThat(multipleSido.matchReason()).contains("복수 대상 시·도");
    }

    @Test
    void regionUnspecifiedIsNotTreatedAsNationwide() {
        Policy policy = policy(1, null, null, LocalDate.of(2026, 7, 1), LocalDate.of(2026, 8, 31));

        PolicyRecommendationMatch match = matcher.match(
                policy,
                RegionCompatibility.REGION_UNSPECIFIED,
                27,
                UserEmploymentStatus.UNEMPLOYED,
                PolicyEmploymentAudience.unknown(),
                LocalDate.of(2026, 7, 24)
        );

        assertThat(match.matched()).isTrue();
        assertThat(match.regionCompatibility()).isEqualTo(RegionCompatibility.REGION_UNSPECIFIED);
        assertThat(match.matchReason()).contains("지역 제한이 명시되지 않은 정책");
        assertThat(match.matchReason()).doesNotContain("전국 대상 정책");
    }

    @Test
    void regionMismatchIsExcludedEvenWhenOtherConditionsMatch() {
        Policy policy = policy(1, null, null, LocalDate.of(2026, 7, 1), LocalDate.of(2026, 8, 31));

        PolicyRecommendationMatch match = matcher.match(
                policy,
                RegionCompatibility.NOT_MATCHED,
                27,
                UserEmploymentStatus.UNEMPLOYED,
                PolicyEmploymentAudience.unknown(),
                LocalDate.of(2026, 7, 24)
        );

        assertThat(match.matched()).isFalse();
    }

    @Test
    void ageMismatchIsExcluded() {
        Policy policy = policy(1, 19, 23, LocalDate.of(2026, 7, 1), LocalDate.of(2026, 8, 31));

        PolicyRecommendationMatch match = matcher.match(
                policy,
                RegionCompatibility.EXACT_SIDO,
                27,
                UserEmploymentStatus.UNEMPLOYED,
                PolicyEmploymentAudience.unknown(),
                LocalDate.of(2026, 7, 24)
        );

        assertThat(match.matched()).isFalse();
    }

    @Test
    void exclusiveEmploymentMismatchIsExcluded() {
        Policy policy = policy(1, null, null, LocalDate.of(2026, 7, 1), LocalDate.of(2026, 8, 31));
        PolicyEmploymentAudience audience =
                new PolicyEmploymentAudience(Set.of(UserEmploymentStatus.UNEMPLOYED), true, 0.9, List.of());

        PolicyRecommendationMatch match = matcher.match(
                policy,
                RegionCompatibility.PARENT_SIDO,
                27,
                UserEmploymentStatus.EMPLOYED,
                audience,
                LocalDate.of(2026, 7, 24)
        );

        assertThat(match.matched()).isFalse();
    }

    @Test
    void expiredPolicyIsExcluded() {
        Policy policy = policy(1, null, null, LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 30));

        PolicyRecommendationMatch match = matcher.match(
                policy,
                RegionCompatibility.NATIONWIDE,
                27,
                UserEmploymentStatus.UNEMPLOYED,
                PolicyEmploymentAudience.unknown(),
                LocalDate.of(2026, 7, 24)
        );

        assertThat(match.matched()).isFalse();
    }

    private Policy policy(Integer id, Integer minAge, Integer maxAge, LocalDate startDate, LocalDate dueDate) {
        Policy policy = new Policy("YC-" + id);
        ReflectionTestUtils.setField(policy, "id", id);
        policy.updateBasic(
                "청년 정책",
                "경기도",
                PolicyCategory.일자리,
                "요약",
                "https://example.com",
                startDate,
                dueDate,
                false,
                true,
                "신청중"
        );
        policy.updateCondition(new PolicyCondition(minAge, maxAge, null, null, null, null, false));
        policy.getRegions().add(new PolicyRegion(policy, region("경기도", "수원시", "CITY")));
        return policy;
    }

    private RegionCode region(String province, String city, String level) {
        return new RegionCode(null, "TEST:" + province + ":" + city, province, city, level);
    }
}
