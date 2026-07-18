package com.weaone.themoa.domain.coaching.service;

import com.weaone.themoa.domain.cardtransaction.repository.CardTransactionRepository;
import com.weaone.themoa.domain.category.entity.Category;
import com.weaone.themoa.domain.category.entity.CategoryCode;
import com.weaone.themoa.domain.coaching.entity.CoachingCardTargetType;
import com.weaone.themoa.domain.coaching.entity.CoachingDismiss;
import com.weaone.themoa.domain.coaching.entity.CoachingDismissType;
import com.weaone.themoa.domain.coaching.repository.CoachingDismissRepository;
import com.weaone.themoa.domain.member.entity.Gender;
import com.weaone.themoa.domain.member.entity.Member;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

/** 습관 코칭 규칙 계층(habitExpense.md §3) 검증. */
@ExtendWith(MockitoExtension.class)
class HabitCoachingCandidateExtractionServiceTest {

    private static final Long MEMBER_ID = 1L;

    @Mock
    private CardTransactionRepository cardTransactionRepository;
    @Mock
    private CoachingDismissRepository coachingDismissRepository;

    private HabitCoachingCandidateExtractionService service() {
        return new HabitCoachingCandidateExtractionService(cardTransactionRepository, coachingDismissRepository);
    }

    private Member member() {
        Member member = Member.signUp("user@example.com", "hash", "닉네임", Gender.MALE, LocalDate.of(2000, 1, 1));
        ReflectionTestUtils.setField(member, "id", MEMBER_ID);
        return member;
    }

    @Test
    @DisplayName("한 카테고리를 alias 하나가 전부 차지하면 카테고리 대신 그 alias를 후보로 승격한다")
    void promotesSingleDominantAliasOverCategory() {
        given(coachingDismissRepository.findByMember_Id(MEMBER_ID)).willReturn(List.of());
        given(cardTransactionRepository.aggregateHabitByCategory(any(), any(), any(), any(), any(), any()))
                .willReturn(List.of(categoryAgg(10L, "DELIVERY", "배달", 9, "363000")));
        given(cardTransactionRepository.aggregateHabitByMerchantAlias(any(), any(), any(), any(), any(), any()))
                .willReturn(List.of(aliasAgg(10L, 20L, "쿠팡이츠", 9, "363000")));

        List<HabitCoachingCandidate> result = service()
                .extractTopCandidates(member(), LocalDate.of(2026, 6, 25), LocalDate.of(2026, 7, 24));

        assertThat(result).hasSize(1);
        assertThat(result.get(0).targetType()).isEqualTo(CoachingCardTargetType.MERCHANT_ALIAS);
        assertThat(result.get(0).merchantAliasId()).isEqualTo(20L);
        assertThat(result.get(0).label()).isEqualTo("쿠팡이츠");
    }

    @Test
    @DisplayName("한 카테고리에 alias가 여러 개 섞여 있으면 카테고리 단위 후보를 그대로 쓴다")
    void keepsCategoryLevelWhenMultipleAliasesShareIt() {
        given(coachingDismissRepository.findByMember_Id(MEMBER_ID)).willReturn(List.of());
        given(cardTransactionRepository.aggregateHabitByCategory(any(), any(), any(), any(), any(), any()))
                .willReturn(List.of(categoryAgg(11L, "CONVENIENCE", "편의점", 82, "393000")));
        given(cardTransactionRepository.aggregateHabitByMerchantAlias(any(), any(), any(), any(), any(), any()))
                .willReturn(List.of(
                        aliasAgg(11L, 21L, "CU", 40, "200000"),
                        aliasAgg(11L, 22L, "GS25", 42, "193000")));

        List<HabitCoachingCandidate> result = service()
                .extractTopCandidates(member(), LocalDate.of(2026, 6, 25), LocalDate.of(2026, 7, 24));

        assertThat(result).hasSize(1);
        assertThat(result.get(0).targetType()).isEqualTo(CoachingCardTargetType.CATEGORY);
        assertThat(result.get(0).categoryId()).isEqualTo(11L);
        assertThat(result.get(0).label()).isEqualTo("편의점");
    }

    @Test
    @DisplayName("월 환산 소비액이 3만원 미만인 후보는 상위권이어도 제외한다")
    void excludesCandidatesBelowMonthlyFloor() {
        given(coachingDismissRepository.findByMember_Id(MEMBER_ID)).willReturn(List.of());
        // 30일 주기에 19,000원 → 월환산도 19,000원(3만원 미만)
        given(cardTransactionRepository.aggregateHabitByCategory(any(), any(), any(), any(), any(), any()))
                .willReturn(List.of(categoryAgg(12L, "TRANSPORT", "택시", 3, "19000")));
        given(cardTransactionRepository.aggregateHabitByMerchantAlias(any(), any(), any(), any(), any(), any()))
                .willReturn(List.of());

        List<HabitCoachingCandidate> result = service()
                .extractTopCandidates(member(), LocalDate.of(2026, 6, 25), LocalDate.of(2026, 7, 24));

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("본인 상대 순위 상위 3개까지만 남기고 나머지는 잘라낸다")
    void limitsToTopThreeByMonthlyAverage() {
        given(coachingDismissRepository.findByMember_Id(MEMBER_ID)).willReturn(List.of());
        given(cardTransactionRepository.aggregateHabitByCategory(any(), any(), any(), any(), any(), any()))
                .willReturn(List.of(
                        categoryAgg(1L, "FOOD", "외식", 10, "475000"),
                        categoryAgg(2L, "CONVENIENCE", "편의점", 10, "393000"),
                        categoryAgg(3L, "DELIVERY", "배달", 10, "363000"),
                        categoryAgg(4L, "CAFE", "카페", 10, "100000")));
        given(cardTransactionRepository.aggregateHabitByMerchantAlias(any(), any(), any(), any(), any(), any()))
                .willReturn(List.of());

        List<HabitCoachingCandidate> result = service()
                .extractTopCandidates(member(), LocalDate.of(2026, 6, 25), LocalDate.of(2026, 7, 24));

        assertThat(result).hasSize(3);
        assertThat(result).extracting(HabitCoachingCandidate::categoryId).containsExactly(1L, 2L, 3L);
    }

    @Test
    @DisplayName("HIDE로 넘긴 카테고리는 후보 풀에서 제외한다")
    void excludesHiddenCategory() {
        Category category = Category.seed(CategoryCode.FOOD, "외식");
        ReflectionTestUtils.setField(category, "id", 1L);
        CoachingDismiss hide = CoachingDismiss.forCategory(member(), category, CoachingDismissType.HIDE,
                java.time.LocalDateTime.now());
        given(coachingDismissRepository.findByMember_Id(MEMBER_ID)).willReturn(List.of(hide));
        given(cardTransactionRepository.aggregateHabitByCategory(any(), any(), any(), any(), any(), any()))
                .willReturn(List.of(categoryAgg(1L, "FOOD", "외식", 10, "475000")));
        given(cardTransactionRepository.aggregateHabitByMerchantAlias(any(), any(), any(), any(), any(), any()))
                .willReturn(List.of());

        List<HabitCoachingCandidate> result = service()
                .extractTopCandidates(member(), LocalDate.of(2026, 6, 25), LocalDate.of(2026, 7, 24));

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("NOT_WASTE로 넘긴 카테고리는 후보에 남되 toneDown 힌트가 켜진다")
    void toneDownsNotWasteCategory() {
        Category category = Category.seed(CategoryCode.FOOD, "외식");
        ReflectionTestUtils.setField(category, "id", 1L);
        CoachingDismiss notWaste = CoachingDismiss.forCategory(member(), category, CoachingDismissType.NOT_WASTE,
                java.time.LocalDateTime.now());
        given(coachingDismissRepository.findByMember_Id(MEMBER_ID)).willReturn(List.of(notWaste));
        given(cardTransactionRepository.aggregateHabitByCategory(any(), any(), any(), any(), any(), any()))
                .willReturn(List.of(categoryAgg(1L, "FOOD", "외식", 10, "475000")));
        given(cardTransactionRepository.aggregateHabitByMerchantAlias(any(), any(), any(), any(), any(), any()))
                .willReturn(List.of());

        List<HabitCoachingCandidate> result = service()
                .extractTopCandidates(member(), LocalDate.of(2026, 6, 25), LocalDate.of(2026, 7, 24));

        assertThat(result).hasSize(1);
        assertThat(result.get(0).toneDown()).isTrue();
    }

    @Test
    @DisplayName("배달 카테고리는 재량 절감비율 0.5, 편의점은 0.3을 적용한다")
    void appliesCategorySavingRatio() {
        given(coachingDismissRepository.findByMember_Id(MEMBER_ID)).willReturn(List.of());
        given(cardTransactionRepository.aggregateHabitByCategory(any(), any(), any(), any(), any(), any()))
                .willReturn(List.of(
                        categoryAgg(3L, "DELIVERY", "배달", 10, "300000"),
                        categoryAgg(2L, "CONVENIENCE", "편의점", 10, "300000")));
        given(cardTransactionRepository.aggregateHabitByMerchantAlias(any(), any(), any(), any(), any(), any()))
                .willReturn(List.of());

        // 30일 주기라 monthlyAverage == netAmount == 300,000
        List<HabitCoachingCandidate> result = service()
                .extractTopCandidates(member(), LocalDate.of(2026, 6, 25), LocalDate.of(2026, 7, 24));

        HabitCoachingCandidate delivery = result.stream().filter(c -> c.categoryId().equals(3L)).findFirst().orElseThrow();
        HabitCoachingCandidate convenience = result.stream().filter(c -> c.categoryId().equals(2L)).findFirst().orElseThrow();
        assertThat(delivery.estimatedSaving()).isEqualByComparingTo("150000");
        assertThat(convenience.estimatedSaving()).isEqualByComparingTo("90000");
    }

    private CardTransactionRepository.HabitCategoryAggregate categoryAgg(Long categoryId, String code, String name,
                                                                           long count, String netAmount) {
        return new CardTransactionRepository.HabitCategoryAggregate() {
            @Override
            public Long getCategoryId() {
                return categoryId;
            }

            @Override
            public String getCategoryCode() {
                return code;
            }

            @Override
            public String getCategoryName() {
                return name;
            }

            @Override
            public long getTransactionCount() {
                return count;
            }

            @Override
            public BigDecimal getNetAmount() {
                return new BigDecimal(netAmount);
            }
        };
    }

    private CardTransactionRepository.HabitMerchantAliasAggregate aliasAgg(Long categoryId, Long aliasId,
                                                                             String aliasName, long count,
                                                                             String netAmount) {
        return new CardTransactionRepository.HabitMerchantAliasAggregate() {
            @Override
            public Long getCategoryId() {
                return categoryId;
            }

            @Override
            public Long getMerchantAliasId() {
                return aliasId;
            }

            @Override
            public String getAliasName() {
                return aliasName;
            }

            @Override
            public long getTransactionCount() {
                return count;
            }

            @Override
            public BigDecimal getNetAmount() {
                return new BigDecimal(netAmount);
            }
        };
    }
}
