package com.weaone.themoa.domain.policy.rag.service;

import com.weaone.themoa.domain.policy.policy.entity.PolicySearchProjection;
import com.weaone.themoa.domain.policy.policy.repository.PolicySearchProjectionRepository;
import com.weaone.themoa.domain.policy.rag.dto.PolicyEmploymentAudience;
import com.weaone.themoa.domain.policy.rag.dto.UserEmploymentStatus;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Collection;
import java.util.EnumSet;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * 정책 신청 대상이 재직자/미취업자 전용인지 판정한다.
 *
 * <p>지원 대상, 신청 자격, 신청 대상/방법, 제목, 설명/사업 내용을 필드별 evidence로 분리해 본다. "19~39세 청년", "서울 청년",
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
            "미취업자|미취업\\s*청년|구직자|구직\\s*등록자|구직\\s*단념\\s*청년|취업\\s*준비생|취업준비생|취준생|현재\\s*미취업\\s*상태|현재\\s*취업하지\\s*않은\\s*자|현재\\s*근로하지\\s*않는\\s*자|현재\\s*재직\\s*중이지\\s*않은\\s*자|고용보험\\s*미가입\\s*미취업자|사업자\\s*등록이\\s*없는\\s*미취업자|고용보험에\\s*가입되지\\s*않은\\s*미취업|실업\\s*상태인\\s*자|취업\\s*경험이\\s*없는\\s*자");
    private static final Pattern EMPLOYED_ONLY = Pattern.compile(
            "재직자|근로자|직장인|중소기업\\s*재직\\s*청년|현재\\s*근무\\s*중인\\s*자|고용보험\\s*가입\\s*근로자|재직\\s*중인\\s*청년");
    private static final Pattern BOTH_OR_NONE = Pattern.compile(
            "취업\\s*여부\\s*무관|재직\\s*여부와\\s*관계없이|근로\\s*여부\\s*무관|재직자\\s*(및|와)\\s*미취업자\\s*모두|미취업자\\s*(및|와)\\s*재직자\\s*모두");
    private static final Pattern AFTER_PARTICIPATION_EMPLOYMENT = Pattern.compile(
            "매칭\\s*후|취업\\s*후|채용\\s*후|근로\\s*경험|일\\s*경험|근무\\s*하게|근로\\s*하게|채용\\s*연계|취업\\s*연계");
    private static final Pattern EMPLOYMENT_TRANSITION_PROGRAM = Pattern.compile(
            "면접\\s*수당|면접비|구직\\s*활동|취업\\s*준비|취업준비|취업\\s*역량|취업\\s*연계|채용\\s*연계|정규직\\s*채용|일자리\\s*매칭|기업.{0,12}매칭|매칭.{0,12}기업");

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
        List<FieldEvidence> allEvidence = new ArrayList<>();
        addEvidence(allEvidence, "지원 대상", projection.getTargetText(), 100);
        addEvidence(allEvidence, "신청 자격", projection.getQualificationText(), 90);
        addEvidence(allEvidence, "신청 대상/방법", projection.getApplicationText(), 80);
        addEvidence(allEvidence, "정책명", projection.getTitleText(), 60);
        addEvidence(allEvidence, "설명/사업 내용", text(projection.getDescriptionText(), projection.getSupportText()), 40);
        String transitionText = text(projection.getTitleText(), projection.getDescriptionText(), projection.getSupportText());
        if (allEvidence.isEmpty()) {
            if (EMPLOYMENT_TRANSITION_PROGRAM.matcher(transitionText).find()) {
                return employmentTransitionProgram();
            }
            return PolicyEmploymentAudience.unknown();
        }
        List<FieldEvidence> unrestricted = allEvidence.stream()
                .filter(FieldEvidence::unrestricted)
                .toList();
        if (!unrestricted.isEmpty()) {
            return new PolicyEmploymentAudience(
                    EnumSet.of(UserEmploymentStatus.EMPLOYED, UserEmploymentStatus.UNEMPLOYED),
                    false,
                    false,
                    false,
                    0.8,
                    unrestricted.stream().map(FieldEvidence::message).toList()
            );
        }
        int highestPriority = allEvidence.stream()
                .map(FieldEvidence::priority)
                .max(Comparator.naturalOrder())
                .orElse(0);
        List<FieldEvidence> strongest = allEvidence.stream()
                .filter(evidence -> evidence.priority() == highestPriority)
                .toList();
        EnumSet<UserEmploymentStatus> strongestStatuses = statuses(strongest);
        if (strongestStatuses.size() == 1) {
            return new PolicyEmploymentAudience(strongestStatuses, true, false,
                    highestPriority >= 80 ? 0.9 : 0.65,
                    strongest.stream().map(FieldEvidence::message).toList());
        }
        EnumSet<UserEmploymentStatus> allStatuses = statuses(allEvidence);
        if (allStatuses.size() > 1) {
            return new PolicyEmploymentAudience(allStatuses, false, true, 0.4,
                    allEvidence.stream().map(FieldEvidence::message).toList());
        }
        if (EMPLOYMENT_TRANSITION_PROGRAM.matcher(transitionText).find()) {
            return employmentTransitionProgram();
        }
        return PolicyEmploymentAudience.unknown();
    }

    private PolicyEmploymentAudience employmentTransitionProgram() {
        return new PolicyEmploymentAudience(Set.of(UserEmploymentStatus.UNKNOWN), false, false, true, 0.55,
                List.of("정책명/설명에 취업 전환·채용 연계 프로그램 표현"));
    }

    private void addEvidence(List<FieldEvidence> evidence, String field, String raw, int priority) {
        if (!StringUtils.hasText(raw)) {
            return;
        }
        if (BOTH_OR_NONE.matcher(raw).find()) {
            evidence.add(new FieldEvidence(null, field, priority, field + "에 취업 상태 무관 표현", true));
            return;
        }
        if (UNEMPLOYED_ONLY.matcher(raw).find()) {
            evidence.add(new FieldEvidence(UserEmploymentStatus.UNEMPLOYED, field, priority, field + "에 미취업자 전용 표현", false));
        }
        if (EMPLOYED_ONLY.matcher(raw).find() && !postParticipationEmploymentOnly(raw, priority)) {
            evidence.add(new FieldEvidence(UserEmploymentStatus.EMPLOYED, field, priority, field + "에 재직자 전용 표현", false));
        }
    }

    private boolean postParticipationEmploymentOnly(String raw, int priority) {
        return priority <= 40 && AFTER_PARTICIPATION_EMPLOYMENT.matcher(raw).find();
    }

    private EnumSet<UserEmploymentStatus> statuses(List<FieldEvidence> evidence) {
        EnumSet<UserEmploymentStatus> statuses = EnumSet.noneOf(UserEmploymentStatus.class);
        evidence.stream()
                .map(FieldEvidence::status)
                .filter(java.util.Objects::nonNull)
                .forEach(statuses::add);
        return statuses;
    }

    private String text(String... values) {
        return String.join(" ", java.util.Arrays.stream(values).map(value -> value == null ? "" : value).toList());
    }

    private record FieldEvidence(UserEmploymentStatus status, String field, int priority, String message, boolean unrestricted) {
    }
}
