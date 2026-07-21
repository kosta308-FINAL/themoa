package com.weaone.themoa.domain.policy.rag.service;

import com.weaone.themoa.domain.policy.policy.entity.PolicySearchProjection;
import com.weaone.themoa.domain.policy.rag.dto.PolicyOfferingType;
import com.weaone.themoa.domain.policy.rag.dto.PolicyOfferingTypeClassification;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * 정책이 사용자에게 제공하는 형태를 분류한다.
 *
 * <p>일반 혜택 검색에서 현금·생활 지원과 인턴십·봉사단 같은 참여형 프로그램을 같은 PRIMARY 목록에
 * 섞지 않기 위한 사용자 표현 계층의 보조 분류기다. 인턴십이라는 제목만으로 취업 상태 자격을
 * Hard Filter하지 않는 대신, 사용자에게는 신청 조건 확인이 필요한 참여형 정책으로 낮춰 보여준다.</p>
 *
 * <p>특정 정책명을 예외 목록으로 두지 않고 title, support, target, qualification, description
 * 텍스트의 일반 표현만 사용한다.</p>
 */
@Component
public class PolicyOfferingTypeClassifier {
    private static final Map<PolicyOfferingType, Pattern> PATTERNS = new LinkedHashMap<>();

    static {
        PATTERNS.put(PolicyOfferingType.STARTUP_FINANCE, Pattern.compile(
                "창업.{0,10}(자금|대출|융자|보증|투자)|창업기업.{0,10}(자금|보증|대출)|사업화\\s*자금"));
        PATTERNS.put(PolicyOfferingType.STARTUP_PROGRAM, Pattern.compile(
                "창업\\s*교육|창업\\s*프로그램|사업화|예비\\s*창업자|창업\\s*지원|창업\\s*컨설팅"));
        PATTERNS.put(PolicyOfferingType.JOB_SEEKER_SUPPORT, Pattern.compile(
                "취업날개|면접\\s*정장|면접\\s*비용|면접\\s*수당|구직\\s*비용|구직\\s*활동|취업\\s*준비"));
        PATTERNS.put(PolicyOfferingType.EMPLOYMENT_OPPORTUNITY, Pattern.compile(
                "인턴십|일경험|채용\\s*연계|취업\\s*연계|직무\\s*체험|청년\\s*인턴|기업\\s*매칭"));
        PATTERNS.put(PolicyOfferingType.EMPLOYEE_BENEFIT, Pattern.compile(
                "재직자\\s*복지|근로\\s*청년|직장인\\s*(복지|지원|혜택)|재직\\s*청년"));
        PATTERNS.put(PolicyOfferingType.CAREER_DEVELOPMENT, Pattern.compile(
                "자격증|응시료|직무\\s*교육|경력\\s*개발|이직\\s*지원|역량\\s*개발"));
        PATTERNS.put(PolicyOfferingType.EDUCATION_PROGRAM, Pattern.compile(
                "교육생\\s*모집|교육\\s*과정|직업\\s*훈련|아카데미|강좌|멘토링\\s*프로그램"));
        PATTERNS.put(PolicyOfferingType.PARTICIPATION_PROGRAM, Pattern.compile(
                "서포터즈|동아리|참가자\\s*모집|공모전|프로젝트\\s*참여자|활동단"));
        PATTERNS.put(PolicyOfferingType.VOLUNTEER_PROGRAM, Pattern.compile(
                "봉사단|해외\\s*봉사|자원\\s*봉사|봉사\\s*활동\\s*참가자"));
        PATTERNS.put(PolicyOfferingType.DIRECT_BENEFIT, Pattern.compile(
                "지원금|수당|보조금|장려금|바우처|현금\\s*지원|월세\\s*지원|주거비\\s*지원|대출|이자\\s*지원|저축\\s*지원|자산\\s*형성|교통비\\s*지원|의료비\\s*지원|할인|계좌|통장"));
        PATTERNS.put(PolicyOfferingType.GENERAL_SERVICE, Pattern.compile(
                "상담|복지\\s*서비스|법률\\s*지원|건강\\s*지원|생활\\s*지원|정보\\s*제공|시설\\s*이용\\s*지원"));
    }

    public PolicyOfferingTypeClassification classify(PolicySearchProjection projection) {
        if (projection == null) {
            return PolicyOfferingTypeClassification.unknown();
        }
        List<FieldText> fields = List.of(
                new FieldText("정책명", projection.getTitleText()),
                new FieldText("지원 내용", projection.getSupportText()),
                new FieldText("지원 대상", projection.getTargetText()),
                new FieldText("신청 자격", projection.getQualificationText()),
                new FieldText("설명", projection.getDescriptionText())
        );
        for (FieldText field : fields) {
            if (!StringUtils.hasText(field.text())) {
                continue;
            }
            for (Map.Entry<PolicyOfferingType, Pattern> entry : PATTERNS.entrySet()) {
                if (entry.getValue().matcher(field.text()).find()) {
                    return new PolicyOfferingTypeClassification(entry.getKey(),
                            List.of(field.name() + "에서 " + label(entry.getKey()) + " 근거 확인"));
                }
            }
        }
        return PolicyOfferingTypeClassification.unknown();
    }

    private String label(PolicyOfferingType type) {
        return switch (type) {
            case DIRECT_BENEFIT -> "직접 혜택";
            case GENERAL_SERVICE -> "생활 지원 서비스";
            case JOB_SEEKER_SUPPORT -> "구직자 지원";
            case EMPLOYMENT_OPPORTUNITY -> "취업·직무 경험";
            case EMPLOYEE_BENEFIT -> "재직자 혜택";
            case CAREER_DEVELOPMENT -> "경력 개발";
            case STARTUP_PROGRAM -> "창업 프로그램";
            case STARTUP_FINANCE -> "창업 자금";
            case EDUCATION_PROGRAM -> "교육·훈련 프로그램";
            case PARTICIPATION_PROGRAM -> "참여 프로그램";
            case VOLUNTEER_PROGRAM -> "봉사 프로그램";
            case UNKNOWN -> "미확인";
        };
    }

    private record FieldText(String name, String text) {
    }
}
