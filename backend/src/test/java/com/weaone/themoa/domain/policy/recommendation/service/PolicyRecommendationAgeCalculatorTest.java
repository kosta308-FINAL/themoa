package com.weaone.themoa.domain.policy.recommendation.service;

import com.weaone.themoa.common.exception.BusinessException;
import com.weaone.themoa.common.exception.ErrorCode;
import com.weaone.themoa.domain.member.entity.Gender;
import com.weaone.themoa.domain.member.entity.Member;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PolicyRecommendationAgeCalculatorTest {
    private final PolicyRecommendationAgeCalculator calculator = new PolicyRecommendationAgeCalculator();

    @Test
    void calculatesAgeBeforeBirthday() {
        assertThat(calculator.ageAt(member(LocalDate.of(1999, 12, 1)), LocalDate.of(2026, 7, 24))).isEqualTo(26);
    }

    @Test
    void calculatesAgeOnBirthday() {
        assertThat(calculator.ageAt(member(LocalDate.of(1999, 7, 24)), LocalDate.of(2026, 7, 24))).isEqualTo(27);
    }

    @Test
    void calculatesAgeAfterBirthday() {
        assertThat(calculator.ageAt(member(LocalDate.of(1999, 3, 12)), LocalDate.of(2026, 7, 24))).isEqualTo(27);
    }

    @Test
    void throwsWhenBirthDateMissing() {
        Member member = member(LocalDate.of(1999, 3, 12));
        ReflectionTestUtils.setField(member, "birthDate", null);

        assertThatThrownBy(() -> calculator.ageAt(member, LocalDate.of(2026, 7, 24)))
                .isInstanceOfSatisfying(BusinessException.class,
                        ex -> assertThat(ex.getErrorCode())
                                .isEqualTo(ErrorCode.POLICY_RECOMMENDATION_BIRTH_DATE_REQUIRED));
    }

    private Member member(LocalDate birthDate) {
        return Member.signUp(
                "user@example.com",
                "password",
                "회원",
                Gender.MALE,
                birthDate,
                LocalDateTime.of(2026, 7, 24, 0, 0)
        );
    }
}
