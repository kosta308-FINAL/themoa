package com.weaone.themoa.domain.coaching.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * 습관 코칭 문구 생성 LLM 계층 구현체(habitExpense.md §4). Gemini(gemini-3.1-flash-lite) — 저비용·소형,
 * 분류+짧은 생성 과제라 추론 모델이 불필요하다는 §6 근거를 따른다. provider 교체 시 이 클래스와
 * application.yaml의 {@code spring.ai.google.genai.*} 설정만 바뀌면 된다(§4 구현 힌트).
 *
 * <p>일시 오류(429·5xx·타임아웃) 재시도는 {@code spring.ai.retry.*}(지수 백오프 최대 3회)로 Spring AI가
 * 처리한다. 여기서는 그 재시도가 전부 소진되거나 영구 오류(400·인증·콘텐츠 거절 등)로 실패한 경우를
 * 한 번에 잡아 빈 리스트를 반환한다 — 호출자가 전량 템플릿으로 폴백한다(§4 "호출 자체가 실패하면 3장
 * 모두 템플릿"). Google GenAI 스타터는 HTTP 클라이언트 타임아웃을 설정 프로퍼티로 노출하지 않아,
 * 응답 타임아웃은 이 클래스가 별도 스레드 + {@link Future#get(long, TimeUnit)}으로 직접 강제한다.
 */
@Slf4j
@Component
public class GeminiHabitCoachingLlmClient implements HabitCoachingLlmClient {

    private static final Duration CALL_TIMEOUT = Duration.ofSeconds(25);

    private static final String SYSTEM_PROMPT = """
            당신은 절약 코칭 카드를 작성하는 어시스턴트다. 아래 규칙을 반드시 지킨다.
            1) 사용자의 소비를 낭비라고 단정하지 않는다. 필수 소비일 가능성을 인정하는 어조를 쓴다(예: "줄일 여지가 있다면").
            2) 사용자 행동을 거울처럼 담담하게 비추는 어조로 쓴다.
            3) 입력으로 주어진 숫자만 그대로 사용한다. 새 숫자를 만들어내지 않는다. body에는 monthlyAverage 또는
               estimatedSaving 숫자를 원 단위 그대로 최소 하나 포함한다.
            4) 입력 후보 하나당 카드 하나를 만든다. 카드는 최대 3장이다.
            5) 대안(대체 수단·행동)을 강요하거나 지시하지 않는다. 언급하더라도 "~해보는 건 어떨까요?" 수준의
               선택 제안까지만 쓴다. "~하세요" 같은 지시형은 쓰지 않는다.
            6) toneDown이 true인 후보는 사용자가 이미 "필요한 소비"로 표시한 항목이다 — 단정적 절약 권유 없이
               담담한 정보 제공 톤으로만 쓴다.
            각 카드는 targetRef, title, body 세 필드만 가진다. targetRef는 입력값을 그대로 되돌려준다.
            """;

    private static final ExecutorService LLM_CALL_EXECUTOR = Executors.newCachedThreadPool(runnable -> {
        Thread thread = new Thread(runnable, "habit-coaching-llm");
        thread.setDaemon(true);
        return thread;
    });

    private final ChatClient.Builder chatClientBuilder;
    private final ObjectMapper objectMapper;

    public GeminiHabitCoachingLlmClient(@Qualifier("googleGenAiChatModel") ChatModel googleGenAiChatModel,
                                         ObjectMapper objectMapper) {
        this.chatClientBuilder = ChatClient.builder(googleGenAiChatModel);
        this.objectMapper = objectMapper;
    }

    @Override
    public List<CoachingCardDraft> generateDrafts(List<HabitCoachingCandidate> candidates) {
        if (candidates.isEmpty()) {
            return List.of();
        }
        try {
            return callWithTimeout(() -> chatClientBuilder.build()
                    .prompt()
                    .system(SYSTEM_PROMPT)
                    .user(writeCandidateJson(candidates))
                    .call()
                    .entity(new ParameterizedTypeReference<List<CoachingCardDraft>>() {
                    }));
        } catch (Exception e) {
            log.warn("습관 코칭 LLM 호출 실패, 이번 주기는 전량 템플릿 카드로 대체합니다.", e);
            return List.of();
        }
    }

    private List<CoachingCardDraft> callWithTimeout(Callable<List<CoachingCardDraft>> call) throws Exception {
        Future<List<CoachingCardDraft>> future = LLM_CALL_EXECUTOR.submit(call);
        try {
            return future.get(CALL_TIMEOUT.toSeconds(), TimeUnit.SECONDS);
        } catch (TimeoutException e) {
            future.cancel(true);
            throw e;
        }
    }

    private String writeCandidateJson(List<HabitCoachingCandidate> candidates) {
        try {
            List<Map<String, Object>> payload = candidates.stream()
                    .map(candidate -> Map.<String, Object>of(
                            "targetRef", candidate.targetRef(),
                            "label", candidate.label(),
                            "transactionCount", candidate.transactionCount(),
                            "totalNetAmount", candidate.totalNetAmount(),
                            "avgPerTransaction", candidate.avgPerTransaction(),
                            "monthlyAverage", candidate.monthlyAverage(),
                            "estimatedSaving", candidate.estimatedSaving(),
                            "toneDown", candidate.toneDown()))
                    .toList();
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("습관 코칭 후보 직렬화에 실패했습니다.", e);
        }
    }
}
