package com.weaone.themoa.domain.policy.rag.service;

import com.weaone.themoa.domain.policy.rag.dto.PolicyOfferingType;
import com.weaone.themoa.domain.policy.rag.dto.UserOfferingIntent;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/**
 * 사용자의 검색어가 일반 혜택 탐색인지, 특정 참여 프로그램 요청인지 판정한다.
 *
 * <p>직장인이라는 조건만으로 인턴십이나 봉사단을 제거하지 않는다. 사용자가 인턴십·해외봉사 같은
 * 프로그램을 직접 요청하면 해당 OfferingType을 다시 허용하고, 그렇지 않은 일반 혜택 검색에서는
 * 참여형 정책을 확인 필요 영역으로 분리한다.</p>
 */
@Component
public class UserOfferingIntentDetector {
    public UserOfferingIntent detect(String query) {
        String normalized = normalize(query);
        if (!StringUtils.hasText(normalized)) {
            return new UserOfferingIntent(Set.of(), false, false, List.of());
        }
        EnumSet<PolicyOfferingType> preferred = EnumSet.noneOf(PolicyOfferingType.class);
        List<String> evidence = new ArrayList<>();
        addIfContains(normalized, preferred, evidence, PolicyOfferingType.EMPLOYMENT_OPPORTUNITY,
                "인턴십", "인턴지원", "일경험사업", "일경험", "직무체험");
        addIfContains(normalized, preferred, evidence, PolicyOfferingType.JOB_SEEKER_SUPPORT,
                "취업지원", "이직지원", "면접", "구직");
        addIfContains(normalized, preferred, evidence, PolicyOfferingType.CAREER_DEVELOPMENT,
                "자격증", "응시료", "직무교육", "경력개발", "이직지원");
        addIfContains(normalized, preferred, evidence, PolicyOfferingType.STARTUP_PROGRAM,
                "창업지원", "창업교육", "사업화", "예비창업");
        addIfContains(normalized, preferred, evidence, PolicyOfferingType.STARTUP_FINANCE,
                "창업대출", "창업자금", "창업보증", "창업기업보증");
        addIfContains(normalized, preferred, evidence, PolicyOfferingType.VOLUNTEER_PROGRAM,
                "해외봉사", "봉사단", "자원봉사", "봉사프로그램");
        addIfContains(normalized, preferred, evidence, PolicyOfferingType.EDUCATION_PROGRAM,
                "교육프로그램", "교육과정", "직업훈련", "아카데미", "강좌");
        addIfContains(normalized, preferred, evidence, PolicyOfferingType.PARTICIPATION_PROGRAM,
                "서포터즈", "동아리", "공모전", "참가자모집");
        boolean explicitProgram = !preferred.isEmpty();
        boolean broadBenefit = containsAny(normalized, "받을수있는혜택", "지원받을수있는것", "받을수있는지원",
                "생활에도움되는정책", "정책혜택", "혜택");
        if (broadBenefit && !explicitProgram) {
            preferred.add(PolicyOfferingType.DIRECT_BENEFIT);
            preferred.add(PolicyOfferingType.GENERAL_SERVICE);
            preferred.add(PolicyOfferingType.EMPLOYEE_BENEFIT);
            evidence.add("일반 혜택 탐색 표현");
        }
        return new UserOfferingIntent(preferred, broadBenefit, explicitProgram, evidence);
    }

    private void addIfContains(String text, EnumSet<PolicyOfferingType> preferred, List<String> evidence,
                               PolicyOfferingType type, String... terms) {
        if (containsAny(text, terms)) {
            preferred.add(type);
            evidence.add(type.name() + " 명시 요청");
        }
    }

    private boolean containsAny(String text, String... terms) {
        for (String term : terms) {
            if (text.contains(term)) return true;
        }
        return false;
    }

    private String normalize(String value) {
        if (value == null) return "";
        return value.toLowerCase()
                .replaceAll("[\\s\\-\\u2010\\u2011\\u2012\\u2013\\u2014\\u2212·ㆍ,()\\[\\]{}<>\"'`~!@#$%^&*_=+|\\\\:;?/.]", "");
    }
}
