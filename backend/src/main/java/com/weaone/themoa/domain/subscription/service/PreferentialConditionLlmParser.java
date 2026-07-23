package com.weaone.themoa.domain.subscription.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.openai.client.OpenAIClient;
import com.openai.client.okhttp.OpenAIOkHttpClient;
import com.openai.models.chat.completions.ChatCompletionCreateParams;
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
 * <p>정규식 파서보다 정확하다 — "최고우대금리 0.45%p"처럼 상한을 안내하는 문구를 조건으로 오인하지 않고,
 * "가/나 중 하나" 같은 구조도 개별 조건으로 풀어낸다.
 *
 * <p>추천 기능(LlmSelector)과 같은 방식으로 OpenAI SDK를 직접 호출한다. themoa는 채팅모델이 둘(google·openai)이라
 * Spring AI의 단일 ChatClient 빈이 만들어지지 않으므로, 검증된 SDK 직접 호출을 쓴다.
 *
 * <p>키가 없거나 호출이 실패하면 null을 반환해, 호출측이 정규식 파서로 폴백한다. 실패가 화면을 막지 않도록
 * 예외를 밖으로 던지지 않는다.
 */
@Component
public class PreferentialConditionLlmParser {

    private static final Logger log = LoggerFactory.getLogger(PreferentialConditionLlmParser.class);
    private static final int MAX_ITEMS = 20;

    private final OpenAiProperties props;
    private volatile OpenAIClient client; // 키 있을 때 최초 1회 생성

    public PreferentialConditionLlmParser(OpenAiProperties props, ObjectMapper objectMapper) {
        this.props = props;
        this.objectMapper = objectMapper;
    }

    private final ObjectMapper objectMapper;

    /** LLM 출력용 구조. rateBonus는 %p 숫자만(예: 0.1). */
    private record LlmItem(String description, BigDecimal rateBonus) {
    }

    /**
     * @return 분해된 조건 목록. 사용할 수 없으면(빈 원문·키 없음·실패) null → 호출측이 정규식으로 폴백
     */
    public List<PreferentialConditionParser.ParsedCondition> parse(String specialCondition) {
        if (!StringUtils.hasText(specialCondition) || !props.hasKey()) {
            return null;
        }
        try {
            String content = callOpenAi(specialCondition);
            LlmItem[] parsed = objectMapper.readValue(stripJsonFence(content), LlmItem[].class);

            List<PreferentialConditionParser.ParsedCondition> items = new ArrayList<>();
            for (LlmItem item : parsed) {
                if (!StringUtils.hasText(item.description())) {
                    continue;
                }
                BigDecimal bonus = item.rateBonus() == null ? BigDecimal.ZERO : item.rateBonus();
                items.add(new PreferentialConditionParser.ParsedCondition(item.description().trim(), bonus));
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

    private String callOpenAi(String specialCondition) {
        String prompt = """
                아래는 은행 예·적금 상품의 우대조건 원문이다. 사용자가 체크리스트로 고를 수 있도록
                "실제로 충족해야 하는 개별 조건"만 뽑아서 JSON 배열로만 출력하라. 다른 설명은 넣지 마라.

                규칙:
                - 각 원소는 {"description": string, "rateBonus": number} 형식. rateBonus는 그 조건 하나가 주는
                  우대금리 %p 숫자만(예: 0.1). 원문에 개별 %p가 없으면 0으로 둔다.
                - "최고우대금리 0.45%p", "최대 연 0.2%p 우대"처럼 전체 상한을 안내하는 문구는 조건이 아니므로 제외한다.
                - "가/나 중 하나" 같은 묶음은 개별 조건으로 각각 풀어서 나열한다.
                - description은 사용자가 이해할 짧은 한국어로 다듬는다(예: "급여이체 월 30만원 이상").
                - 조건이 하나도 없으면 빈 배열 [] 을 출력한다.

                우대조건 원문:
                %s""".formatted(specialCondition);

        ChatCompletionCreateParams params = ChatCompletionCreateParams.builder()
                .model(props.model())
                .addUserMessage(prompt)
                .build();

        return client().chat().completions().create(params).choices().stream()
                .flatMap(choice -> choice.message().content().stream())
                .findFirst()
                .orElse("[]");
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

    private String stripJsonFence(String content) {
        if (content == null) {
            return "[]";
        }
        String trimmed = content.trim();
        if (trimmed.startsWith("```")) {
            trimmed = trimmed.replaceFirst("^```json", "").replaceFirst("^```", "");
            int end = trimmed.lastIndexOf("```");
            if (end >= 0) {
                trimmed = trimmed.substring(0, end);
            }
        }
        return trimmed.trim();
    }
}
