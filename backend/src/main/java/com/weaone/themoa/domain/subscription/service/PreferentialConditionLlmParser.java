package com.weaone.themoa.domain.subscription.service;

import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.openai.client.OpenAIClient;
import com.openai.client.okhttp.OpenAIOkHttpClient;
import com.openai.models.chat.completions.ChatCompletionCreateParams;
import com.openai.models.chat.completions.StructuredChatCompletionCreateParams;
import com.weaone.themoa.domain.recommend.service.OpenAiProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * 우대조건 원문을 LLM으로 "충족 조건 + 가산금리(%p)" 항목으로 분해한다.
 *
 * <p>추천 기능(LlmSelector)과 같은 방식으로 OpenAI의 <b>구조화 출력(Structured Outputs)</b>을 쓴다.
 * 응답을 {@link Extracted} 스키마로 강제하므로 모델이 형식을 어길 수 없어, "JSON이 아니라 파싱 실패 →
 * 정규식 폴백"이 나지 않는다.
 *
 * <p>정규식 파서로는 못 잡던 문제들(줄바꿈으로 쪼개진 조건, 은행마다 다른 글머리, "최고 …%p" 상한 안내)을
 * 프롬프트와 필드 설명으로 처리한다. 키가 없거나 호출이 실패하면 null을 반환해 정규식 파서로 폴백한다.
 */
@Component
public class PreferentialConditionLlmParser {

    private static final Logger log = LoggerFactory.getLogger(PreferentialConditionLlmParser.class);
    private static final int MAX_ITEMS = 20;

    private final OpenAiProperties props;
    private volatile OpenAIClient client;

    public PreferentialConditionLlmParser(OpenAiProperties props) {
        this.props = props;
    }

    /** 구조화 출력 스키마. 필드 설명이 그대로 모델에 전달되어 추출 규칙 역할을 한다. */
    public static class Extracted {
        @JsonPropertyDescription("""
                사용자가 체크리스트로 고를 수 있는 '개별 우대조건'만 담는다.
                - '최고 연 3%p', '최대 …우대', '우대금리 최고 …' 처럼 전체 상한/합계를 안내하는 문구는 제외한다(조건 아님).
                - 원문이 줄바꿈으로 쪼개져 있어도 앞뒤 줄을 합쳐 하나의 온전한 조건 문장으로 만든다.
                - 글머리 기호(가., 1., ①, - 등)는 떼고 핵심 내용만 남긴다.
                - 조건이 하나도 없으면 빈 배열.""")
        public List<Item> conditions;

        public static class Item {
            @JsonPropertyDescription("이 조건을 사용자가 이해할 짧은 한국어 문장(예: '급여이체 월 50만원 이상 6개월')")
            public String description;

            @JsonPropertyDescription("이 조건 하나가 주는 우대금리 %p 숫자만(예: 1.0). 원문에 개별 %p가 없으면 0")
            public BigDecimal rateBonus;
        }
    }

    /**
     * @return 분해된 조건 목록. 사용할 수 없으면(빈 원문·키 없음·실패) null → 호출측이 정규식으로 폴백
     */
    public List<PreferentialConditionParser.ParsedCondition> parse(String specialCondition) {
        if (!StringUtils.hasText(specialCondition) || !props.hasKey()) {
            return null;
        }
        try {
            Extracted extracted = callOpenAi(specialCondition);
            if (extracted == null || extracted.conditions == null) {
                return null;
            }
            List<PreferentialConditionParser.ParsedCondition> items = new ArrayList<>();
            for (Extracted.Item item : extracted.conditions) {
                if (item == null || !StringUtils.hasText(item.description)) {
                    continue;
                }
                BigDecimal bonus = item.rateBonus == null ? BigDecimal.ZERO : item.rateBonus;
                items.add(new PreferentialConditionParser.ParsedCondition(item.description.trim(), bonus));
                if (items.size() >= MAX_ITEMS) {
                    break;
                }
            }
            return items;
        } catch (Exception e) {
            log.warn("우대조건 LLM 파싱 실패 → 정규식으로 폴백: {}", e.toString());
            return null;
        }
    }

    private Extracted callOpenAi(String specialCondition) {
        String prompt = """
                아래는 은행 예·적금 상품의 우대조건 원문이다. 사용자가 체크리스트로 고를 수 있도록 실제로
                충족해야 하는 개별 우대조건만 뽑아라.

                원문:
                %s""".formatted(specialCondition);

        StructuredChatCompletionCreateParams<Extracted> params = ChatCompletionCreateParams.builder()
                .model(props.model())
                .addUserMessage(prompt)
                .responseFormat(Extracted.class)
                .build();

        return client().chat().completions().create(params).choices().stream()
                .flatMap(choice -> choice.message().content().stream())
                .findFirst()
                .orElse(null);
    }

    private OpenAIClient client() {
        if (client == null) {
            synchronized (this) {
                if (client == null) {
                    client = OpenAIOkHttpClient.builder().apiKey(props.apiKey()).build();
                }
            }
        }
        return client;
    }
}
