package com.weaone.themoa.domain.policy.rag.service;

import com.weaone.themoa.domain.policy.rag.dto.BenefitGroup;
import com.weaone.themoa.domain.policy.rag.dto.SearchDomain;
import com.weaone.themoa.domain.policy.rag.dto.SupportIntent;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/**
 * 사용자 질문의 세부 지원 형태를 넓은 혜택 그룹으로 정규화한다.
 *
 * <p>SupportIntent는 정책 태그와 관리자 진단에 필요한 세부 신호지만, 사용자 검색에서 그대로 쓰면
 * "지원금" 질문이 현금 지급 정책에만 좁게 고정된다. 이 감지기는 저축, 대출, 자산형성, 주거비,
 * 교통비처럼 실질적으로 경제 부담을 줄이는 정책을 ECONOMIC_SUPPORT로 묶어 후보 확장과 ranking에 전달한다.</p>
 */
@Component
public class BenefitGroupDetector {
    public Detection detect(String query,
                            Set<String> supportTypes,
                            Set<String> positiveKeywords,
                            Set<SupportIntent> supportIntents,
                            Set<SearchDomain> desiredDomains) {
        Set<BenefitGroup> groups = EnumSet.noneOf(BenefitGroup.class);
        List<String> evidence = new ArrayList<>();
        String text = compact(String.join(" ",
                query == null ? "" : query,
                join(supportTypes),
                join(positiveKeywords)));

        if (economicByText(text) || containsEconomicSupportIntent(supportIntents)) {
            groups.add(BenefitGroup.ECONOMIC_SUPPORT);
            evidence.add("economic:query/supportIntent");
        }
        if (containsAny(text, "주거", "월세", "전세", "임차료", "주거비") || contains(supportIntents, SupportIntent.HOUSING_COST)
                || contains(desiredDomains, SearchDomain.HOUSING)) {
            groups.add(BenefitGroup.HOUSING_SUPPORT);
            evidence.add("housing:query/domain");
        }
        if (containsAny(text, "취업지원", "이직지원", "면접", "구직", "자격증", "직무교육", "경력개발")
                || contains(supportIntents, SupportIntent.EMPLOYMENT_SUPPORT) || contains(desiredDomains, SearchDomain.EMPLOYMENT)) {
            groups.add(BenefitGroup.EMPLOYMENT_SUPPORT);
            evidence.add("employment:query/domain");
        }
        if (containsAny(text, "교육지원", "교육비", "훈련", "강좌", "학습", "대학생")
                || contains(supportIntents, SupportIntent.EDUCATION) || contains(desiredDomains, SearchDomain.EDUCATION)) {
            groups.add(BenefitGroup.EDUCATION_SUPPORT);
            evidence.add("education:query/domain");
        }
        if (containsAny(text, "혜택", "지원정책", "지원받", "받을수있는", "도움되는정책", "청년정책")) {
            groups.add(BenefitGroup.GENERAL_BENEFIT);
            evidence.add("general:benefit-expression");
        }
        return new Detection(groups.isEmpty() ? Set.of() : Set.copyOf(groups), List.copyOf(evidence));
    }

    private boolean economicByText(String text) {
        return containsAny(text,
                "지원금", "금전적혜택", "금전지원", "현금지원", "현금성혜택", "돈으로지원", "받을수있는돈",
                "수당", "생활수당", "활동수당", "보조금", "장려금", "생활비", "활동비",
                "경제적으로도움", "경제적부담", "부담을줄", "비용지원", "비용환급", "비용경감", "환급", "할인",
                "자산형성", "저축", "계좌", "통장", "정부기여금", "정부기여", "매칭적립", "적립",
                "대출", "융자", "이자지원", "이자보전", "보증지원", "보증",
                "월세", "주거비", "임차료", "교통비", "대중교통", "교통패스", "K-패스", "K패스", "k-패스", "k패스", "케이패스", "바우처");
    }

    private boolean containsEconomicSupportIntent(Set<SupportIntent> supportIntents) {
        if (supportIntents == null || supportIntents.isEmpty()) {
            return false;
        }
        return supportIntents.stream().anyMatch(intent -> switch (intent) {
            case CASH_ASSISTANCE, ALLOWANCE, LOAN, SAVINGS, MATCHED_SAVINGS, ASSET_BUILDING, HOUSING_COST -> true;
            default -> false;
        });
    }

    private boolean contains(Set<?> values, Object target) {
        return values != null && values.contains(target);
    }

    private String join(Set<String> values) {
        return values == null ? "" : String.join(" ", values);
    }

    private String compact(String value) {
        return StringUtils.hasText(value) ? value.replaceAll("\\s+", "") : "";
    }

    private boolean containsAny(String text, String... needles) {
        for (String needle : needles) {
            if (text.contains(needle)) {
                return true;
            }
        }
        return false;
    }

    public record Detection(Set<BenefitGroup> groups, List<String> evidence) {
    }
}
