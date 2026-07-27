package com.weaone.themoa.domain.policy.rag.service;

import com.weaone.themoa.domain.policy.policy.entity.PolicySearchProjection;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class OpenAiPolicyGenderAiClient implements PolicyGenderAiClient {
    private final ObjectProvider<ChatModel> openAiChatModelProvider;
    private final String openAiApiKey;

    public OpenAiPolicyGenderAiClient(@Qualifier("openAiChatModel") ObjectProvider<ChatModel> openAiChatModelProvider,
                                      @Value("${spring.ai.openai.api-key:}") String openAiApiKey) {
        this.openAiChatModelProvider = openAiChatModelProvider;
        this.openAiApiKey = openAiApiKey;
    }

    @Override
    public PolicyGenderAiAnalysis analyze(PolicySearchProjection projection) {
        if (!StringUtils.hasText(openAiApiKey)) {
            return null;
        }
        ChatModel chatModel = openAiChatModelProvider.getIfAvailable();
        if (chatModel == null) {
            return null;
        }
        return ChatClient.builder(chatModel).build()
                .prompt()
                .system("""
                        대한민국 청년 정책 문서에서 실제 신청자 또는 직접 수혜자인 개인의 성별 자격을 판단한다.
                        문서에 남성 또는 여성 단어가 있다는 이유만으로 제한을 확정하지 않는다.
                        정책 주관기관 이름은 신청자의 성별 근거가 아니다.
                        기업 대표, 사업체 유형, 근로자, 가족, 배우자, 보호자와 실제 신청자를 구분한다.
                        여성기업, 여성 대표 기업, 여성가족부, 성평등 사업은 자동으로 FEMALE_ONLY가 아니다.
                        개인 신청자가 특정 성별로 명확히 제한될 때만 MALE_ONLY 또는 FEMALE_ONLY로 판정한다.
                        성별과 관계없이 개인이 신청 가능하면 ALL이다.
                        근거가 부족하거나 신청 주체가 불명확하면 UNKNOWN이다.
                        추측하지 않는다.
                        evidence에는 원문을 길게 복사하지 않고 판정 이유만 짧게 작성한다.
                        audience 값은 ALL, MALE_ONLY, FEMALE_ONLY, UNKNOWN 중 하나다.
                        applicantScope 값은 INDIVIDUAL, BUSINESS, ORGANIZATION, FAMILY, GUARDIAN, SPOUSE, UNKNOWN 중 하나다.
                        """)
                .user(policyText(projection))
                .call()
                .entity(PolicyGenderAiAnalysis.class);
    }

    private String policyText(PolicySearchProjection projection) {
        return """
                titleText: %s
                targetText: %s
                qualificationText: %s
                applicationText: %s
                descriptionText: %s
                supportText: %s
                institutionText: %s
                """.formatted(
                nullToEmpty(projection.getTitleText()),
                nullToEmpty(projection.getTargetText()),
                nullToEmpty(projection.getQualificationText()),
                nullToEmpty(projection.getApplicationText()),
                nullToEmpty(projection.getDescriptionText()),
                nullToEmpty(projection.getSupportText()),
                nullToEmpty(projection.getInstitutionText())
        );
    }

    private String nullToEmpty(String value) {
        return value == null ? "" : value;
    }
}
