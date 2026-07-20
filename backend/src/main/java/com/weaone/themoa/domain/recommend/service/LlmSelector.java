package com.weaone.themoa.domain.recommend.service;

import com.weaone.themoa.domain.recommend.dto.Recommendation;
import com.weaone.themoa.domain.recommend.dto.UserProfile;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.openai.client.OpenAIClient;
import com.openai.client.okhttp.OpenAIOkHttpClient;
import com.openai.models.chat.completions.ChatCompletionCreateParams;
import com.openai.models.chat.completions.StructuredChatCompletionCreateParams;

/**
 * 3단계 - 순위(=점수)는 규칙이 이미 확정한 상태로 넘어온다.
 * LLM(OpenAI)은 그 순서를 바꾸지 않고, 각 상품에 자연어 추천 이유만 붙인다.
 * ("점수 1등이 곧 1위"를 보장하기 위해 LLM에 선택·재정렬 권한을 주지 않는다.)
 * 키가 없거나 호출이 실패하면 null을 반환해 호출측이 규칙 이유(reasons)만으로 폴백한다.
 */
@Service
public class LlmSelector {

    private static final Logger log = LoggerFactory.getLogger(LlmSelector.class);

    private final OpenAiProperties props;
    private volatile OpenAIClient client;   // 키 있을 때 최초 1회 생성

    public LlmSelector(OpenAiProperties props) {
        this.props = props;
    }

    public boolean isEnabled() {
        return props.hasKey();
    }

    /** 상품별 자연어 이유 목록(구조화 출력용, OpenAI structured outputs가 이 POJO를 그대로 채운다). */
    public static class Explanations {
        @JsonPropertyDescription("입력받은 상품 전부에 대해, 순서와 개수를 그대로 유지한 이유 목록")
        public List<Item> items;

        public static class Item {
            @JsonPropertyDescription("입력 목록의 회사명과 정확히 동일하게")
            public String company;
            @JsonPropertyDescription("입력 목록의 상품명과 정확히 동일하게")
            public String productName;
            @JsonPropertyDescription("이 상품이 왜 이 사용자에게 좋은지 친근한 말투로 1~2문장 한국어")
            public String reason;
        }
    }

    /**
     * 이미 순위가 확정된 상품 목록(recs)에 자연어 이유를 붙인다. 순서·개수는 절대 바꾸지 않는다.
     * 실패/비활성 시 null(→ 호출측이 규칙 이유만으로 폴백).
     */
    public List<Recommendation> explain(UserProfile profile, List<Recommendation> recs) {
        if (!isEnabled() || recs.isEmpty()) {
            return null;
        }
        try {
            Explanations explanations = callOpenAi(profile, recs);
            if (explanations == null || explanations.items == null) {
                return null;
            }
            Map<String, String> reasonByKey = new HashMap<>();
            for (Explanations.Item item : explanations.items) {
                reasonByKey.put(key(item.company, item.productName), item.reason);
            }
            // 원래 순서(recs) 그대로, 이유만 있으면 붙인다. 순서·개수는 절대 바뀌지 않는다.
            return recs.stream()
                    .map(r -> {
                        String reason = reasonByKey.get(key(r.company(), r.productName()));
                        return reason == null ? r : r.withLlmReason(reason);
                    })
                    .toList();
        } catch (Exception e) {
            log.warn("LLM 이유 생성 실패 → 규칙 이유로 대체: {}", e.toString());
            return null;
        }
    }

    private Explanations callOpenAi(UserProfile profile, List<Recommendation> recs) {
        StructuredChatCompletionCreateParams<Explanations> params = ChatCompletionCreateParams.builder()
                .model(props.model())
                .addUserMessage(buildPrompt(profile, recs))
                .responseFormat(Explanations.class)
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

    private static String key(String company, String productName) {
        return company + "|" + productName;
    }

    private static String buildPrompt(UserProfile p, List<Recommendation> recs) {
        StringBuilder sb = new StringBuilder();
        sb.append("사회초년생 금융상품 추천 도우미다. 아래 상품들은 이미 점수 순위가 확정된 최종 추천 목록이다. ")
                .append("순서를 바꾸거나 상품을 빼지 말고, 각 상품마다 이 사용자에게 왜 좋은지 이유만 작성해줘.\n\n")
                .append("각 상품에는 실제 점수를 매긴 근거(rule reasons)와 구체적인 예상 금액을 같이 줄 텐데, ")
                .append("이건 너가 '왜 이 상품이 상위인지' 이해하라고 주는 내부 참고자료일 뿐이다 — ")
                .append("절대 사용자에게 보여줄 문장에 그대로 옮기면 안 된다. ")
                .append("특히 \"(+8)\", \"(+15)\" 같은 점수 표기나 \"우대조건 까다롭지만 감수 가능\", ")
                .append("\"목표기간에 매우 근접한 상품\" 같은 내부 라벨 문구를 절대 그대로 인용하지 마 — ")
                .append("이건 개발자용 채점 근거지 사용자용 설명이 아니다. 그 근거가 '왜' 좋은지를 ")
                .append("사용자가 이해할 자연스러운 말로 완전히 풀어써야 한다(예: 내부 라벨이 '우대조건 까다롭지만 ")
                .append("감수 가능 (+8)'이면 → '우대조건을 채우면 더 높은 금리를 받을 수 있어요' 식으로).\n")
                .append("숫자는 예상만기금액·금리%처럼 사용자가 실제로 이해할 수 있는 것만 문장에 자연스럽게 녹여써. ")
                .append("점수(score)나 (+N) 표기 자체는 절대 언급하지 마. ")
                .append("\"안정적으로 자산을 늘릴 수 있어요\", \"좋은 선택이에요\"처럼 아무 상품에나 붙일 수 있는 ")
                .append("일반론적인 문장만 쓰지는 말고, 왜 다른 상품이 아니라 '이' 상품인지가 드러나게 써.\n\n");
        sb.append("[사용자]\n")
                .append("- 나이: ").append(p.age()).append("세\n")
                .append("- 월소득(만원): ").append(p.monthlyIncomeManwon()).append("\n")
                .append("- 취업유형: ").append(p.employmentType()).append("\n")
                .append("- 위험성향: ").append(p.riskType()).append("\n")
                .append("- 목표 가입기간: ").append(p.effectiveTargetMonths()).append("개월\n")
                .append("- 월 납입가능(원): ").append(p.monthlyDepositWon()).append("\n")
                .append("- 우대조건 감수: ").append(p.acceptCondition() ? "가능" : "싫음").append("\n");
        if (p.goalAmountWon() != null) {
            sb.append("- 저축목표: ").append(p.goalAmountWon()).append("원");
            if (p.goalMonths() != null) {
                sb.append(" / ").append(p.goalMonths()).append("개월");
            }
            sb.append(" (목표 달성 가능 여부는 화면 상단에 이미 별도로 안내되니, 상품별 이유에는 ")
                    .append("절대 반복해서 언급하지 마. 이유는 오직 그 상품 자체의 장점에만 집중해.)");
            sb.append("\n");
        }
        sb.append("\n");

        sb.append("[최종 추천 목록] (순위 순서, 전부 이유를 작성할 것)\n");
        int i = 1;
        for (Recommendation r : recs) {
            sb.append(i++).append("위. ").append(r.company()).append(" / ").append(r.productName())
                    .append(" (").append("DEPOSIT".equals(r.type()) ? "예금" : "적금")
                    .append(", 금리 ").append(r.bestRate()).append("%")
                    .append(r.bestRateTerm() == null ? "" : " " + r.bestRateTerm() + "개월")
                    .append(", 점수 ").append(r.score()).append(")\n");
            if (r.maturityAmountWon() != null) {
                sb.append("   - 여력 전액 납입시 예상 만기금액: ").append(r.maturityAmountWon()).append("원\n");
            }
            if (r.goalMonthlyWon() != null) {
                sb.append("   - 목표 채우는 데 필요한 월 최소금액: ").append(r.goalMonthlyWon()).append("원\n");
            }
            sb.append("   - 점수 근거: ").append(String.join(" / ", r.reasons())).append("\n");
        }
        sb.append("\n각 상품마다 친근한 말투로 1~2문장 이유를 써줘. 위에 준 점수 근거나 금액 중 최소 하나는 ")
                .append("구체적으로 녹여서 써야 해. 목록에 없는 상품은 절대 만들지 마.");
        return sb.toString();
    }
}
