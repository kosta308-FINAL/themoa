package com.weaone.themoa.domain.policy.rag.service;

import com.weaone.themoa.domain.policy.policy.domain.PolicySearchProjection;
import com.weaone.themoa.domain.policy.policy.repository.PolicySearchProjectionRepository;
import com.weaone.themoa.domain.policy.rag.dto.PolicyEmploymentAudience;
import com.weaone.themoa.domain.policy.rag.dto.UserEmploymentStatus;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Collection;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * 정책 신청 대상이 재직자/미취업자 전용인지 판정한다.
 *
 * <p>신청 자격·추가 자격·제외 조건을 지원 대상보다 먼저 본다. "19~39세 청년", "서울 청년",
 * "청년 누구나" 같은 일반 청년 표현은 취업 상태 무관 근거가 아니며, 명시적인 미취업/재직 제한을
 * 덮어쓰면 안 된다. 정말 "취업 여부 무관", "근로 여부 무관"처럼 적힌 경우에만 양쪽 상태를 허용한다.</p>
 *
 * <p>정책 설명의 취업 주제어만으로 자격 상태를 단정하지 않는다.
 * 특히 제목에 인턴십·취업·면접·일경험이 있다는 이유만으로 미취업자 전용 Hard Filter를 적용하면
 * 실제로 직장인이 찾는 프로그램 검색까지 제거될 수 있다. 따라서 명시적인 대상/자격 표현이 있을 때만
 * exclusive 취업 상태를 반환한다.</p>
 */
@Component
public class PolicyEmploymentAudienceClassifier {
    private static final Pattern UNEMPLOYED_ONLY = Pattern.compile(
            "미취업자|미취업\\s*청년|구직자|취업\\s*준비생|취준생|현재\\s*취업하지\\s*않은\\s*자|현재\\s*근로하지\\s*않는\\s*자|현재\\s*재직\\s*중이지\\s*않은\\s*자|고용보험\\s*미가입\\s*미취업자|사업자\\s*등록이\\s*없는\\s*미취업자|고용보험에\\s*가입되지\\s*않은\\s*미취업");
    private static final Pattern EMPLOYED_ONLY = Pattern.compile(
            "재직자|근로자|직장인|중소기업\\s*재직\\s*청년|현재\\s*근무\\s*중인\\s*자|고용보험\\s*가입\\s*근로자");
    private static final Pattern BOTH_OR_NONE = Pattern.compile(
            "취업\\s*여부\\s*무관|재직\\s*여부와\\s*관계없이|근로\\s*여부\\s*무관|재직자\\s*(및|와)\\s*미취업자\\s*모두|미취업자\\s*(및|와)\\s*재직자\\s*모두");

    private final PolicySearchProjectionRepository projectionRepository;

    public PolicyEmploymentAudienceClassifier(PolicySearchProjectionRepository projectionRepository) {
        this.projectionRepository = projectionRepository;
    }

    public Map<Integer, PolicyEmploymentAudience> classify(Collection<Integer> policyIds) {
        if (policyIds == null || policyIds.isEmpty()) {
            return Map.of();
        }
        Map<Integer, PolicyEmploymentAudience> result = new LinkedHashMap<>();
        projectionRepository.findAllById(policyIds).forEach(projection ->
                result.put(projection.getPolicyId(), classify(projection)));
        return result;
    }

    public PolicyEmploymentAudience classify(PolicySearchProjection projection) {
        if (projection == null) {
            return PolicyEmploymentAudience.unknown();
        }
        String qualification = text(projection.getQualificationText(), projection.getApplicationText());
        String target = text(projection.getTargetText());
        String employedSupport = text(projection.getTitleText(), projection.getDescriptionText(), projection.getSupportText());
        EnumSet<UserEmploymentStatus> statuses = EnumSet.noneOf(UserEmploymentStatus.class);
        java.util.ArrayList<String> evidence = new java.util.ArrayList<>();
        if (UNEMPLOYED_ONLY.matcher(qualification).find()) {
            statuses.add(UserEmploymentStatus.UNEMPLOYED);
            evidence.add("신청 자격에 미취업자 전용 표현");
        }
        if (EMPLOYED_ONLY.matcher(qualification).find()) {
            statuses.add(UserEmploymentStatus.EMPLOYED);
            evidence.add("신청 자격에 재직자 전용 표현");
        }
        if (statuses.isEmpty() && UNEMPLOYED_ONLY.matcher(target).find()) {
            statuses.add(UserEmploymentStatus.UNEMPLOYED);
            evidence.add("지원 대상에 미취업자 전용 표현");
        }
        if (statuses.isEmpty() && EMPLOYED_ONLY.matcher(target).find()) {
            statuses.add(UserEmploymentStatus.EMPLOYED);
            evidence.add("지원 대상에 재직자 전용 표현");
        }
        String strong = text(qualification, target);
        if (statuses.isEmpty() && BOTH_OR_NONE.matcher(strong).find()) {
            statuses.add(UserEmploymentStatus.EMPLOYED);
            statuses.add(UserEmploymentStatus.UNEMPLOYED);
            evidence.add("취업 상태 제한 없음 명시");
            return new PolicyEmploymentAudience(statuses, false, 0.8, evidence);
        }
        if (statuses.isEmpty() && EMPLOYED_ONLY.matcher(employedSupport).find()) {
            statuses.add(UserEmploymentStatus.EMPLOYED);
            evidence.add("정책명/설명에 재직자 대상 표현");
        }
        if (statuses.isEmpty()) {
            return PolicyEmploymentAudience.unknown();
        }
        boolean exclusive = statuses.size() == 1;
        return new PolicyEmploymentAudience(statuses, exclusive,
                StringUtils.hasText(qualification) || StringUtils.hasText(target) ? 0.9 : 0.65, evidence);
    }

    private String text(String... values) {
        return String.join(" ", java.util.Arrays.stream(values).map(value -> value == null ? "" : value).toList());
    }
}
