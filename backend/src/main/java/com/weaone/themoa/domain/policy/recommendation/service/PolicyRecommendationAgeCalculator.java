package com.weaone.themoa.domain.policy.recommendation.service;

import com.weaone.themoa.common.exception.BusinessException;
import com.weaone.themoa.common.exception.ErrorCode;
import com.weaone.themoa.domain.member.entity.Member;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.Period;
import java.time.ZoneId;

@Component
public class PolicyRecommendationAgeCalculator {
    private static final ZoneId SEOUL_ZONE = ZoneId.of("Asia/Seoul");

    public int currentAge(Member member) {
        return ageAt(member, LocalDate.now(SEOUL_ZONE));
    }

    int ageAt(Member member, LocalDate today) {
        if (member.getBirthDate() == null) {
            throw new BusinessException(ErrorCode.POLICY_RECOMMENDATION_BIRTH_DATE_REQUIRED);
        }
        return Period.between(member.getBirthDate(), today).getYears();
    }
}
