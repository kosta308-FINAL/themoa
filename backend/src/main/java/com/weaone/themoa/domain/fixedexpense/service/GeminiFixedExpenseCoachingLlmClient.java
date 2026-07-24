package com.weaone.themoa.domain.fixedexpense.service;

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
 * 고정지출 코칭 카드 LLM 계층 구현체. 습관 코칭({@code GeminiHabitCoachingLlmClient})과 동일한 모델·설정
 * (googleGenAiChatModel)을 공유한다 — 분류+짧은 생성 과제라 추론 모델이 불필요하다는 근거도 동일하다.
 */
@Slf4j
@Component
public class GeminiFixedExpenseCoachingLlmClient implements FixedExpenseCoachingLlmClient {

    private static final Duration CALL_TIMEOUT = Duration.ofSeconds(25);
    private static final int MAX_CARDS = 3;

    private static final String SYSTEM_PROMPT = """
            당신은 고정지출 목록에서 "연 환산 안내 카드"에 올릴 항목을 고르는 어시스턴트다. 아래 규칙을 반드시 지킨다.
            1) 월세, 관리비, 보험료, 대출상환, 통신비, 세금처럼 생활에 필수적이거나 의무적인 성격의 지출은
               이름과 카테고리를 보고 절대 고르지 않는다. 카테고리가 "기타"로 애매하게 등록돼 있어도 이름에서
               필수 지출로 읽히면 제외한다.
            2) 구독 서비스, OTT, 취미·여가성 정기결제처럼 재량으로 조정 가능한 항목 위주로 최대 3개까지만 고른다.
            3) 절감을 종용하지 않는다. "~을 줄이세요", "해지하세요" 같은 지시형 표현을 절대 쓰지 않는다. 그저
               "한 달 OOO원이지만 1년으로 보면 OOO원이에요" 수준의 담담한 정보 제공 톤만 쓴다.
            4) 입력으로 주어진 monthlyAmount·annualAmount 숫자만 그대로 사용한다. 새 숫자를 만들어내지 않는다.
               body에는 annualAmount 숫자를 원 단위 그대로 반드시 포함한다.
            5) 후보 전부가 필수 지출로 판단되면 빈 리스트를 반환해도 된다 — 무리해서 채우지 않는다.
            각 카드는 targetRef, title, body 세 필드만 가진다. targetRef는 입력값을 그대로 되돌려준다.
            """;

    private static final ExecutorService LLM_CALL_EXECUTOR = Executors.newCachedThreadPool(runnable -> {
        Thread thread = new Thread(runnable, "fixed-expense-coaching-llm");
        thread.setDaemon(true);
        return thread;
    });

    private final ChatClient.Builder chatClientBuilder;
    private final ObjectMapper objectMapper;

    public GeminiFixedExpenseCoachingLlmClient(@Qualifier("googleGenAiChatModel") ChatModel googleGenAiChatModel,
                                                ObjectMapper objectMapper) {
        this.chatClientBuilder = ChatClient.builder(googleGenAiChatModel);
        this.objectMapper = objectMapper;
    }

    @Override
    public List<FixedExpenseCoachingDraft> selectAndDraft(List<FixedExpenseCoachingCandidate> candidates) {
        if (candidates.isEmpty()) {
            return List.of();
        }
        try {
            List<FixedExpenseCoachingDraft> drafts = callWithTimeout(() -> chatClientBuilder.build()
                    .prompt()
                    .system(SYSTEM_PROMPT)
                    .user(writeCandidateJson(candidates))
                    .call()
                    .entity(new ParameterizedTypeReference<List<FixedExpenseCoachingDraft>>() {
                    }));
            return drafts.size() > MAX_CARDS ? drafts.subList(0, MAX_CARDS) : drafts;
        } catch (Exception e) {
            log.warn("고정지출 코칭 LLM 호출 실패, 이번 주기는 카드 없이 넘어갑니다.", e);
            return List.of();
        }
    }

    private List<FixedExpenseCoachingDraft> callWithTimeout(Callable<List<FixedExpenseCoachingDraft>> call)
            throws Exception {
        Future<List<FixedExpenseCoachingDraft>> future = LLM_CALL_EXECUTOR.submit(call);
        try {
            return future.get(CALL_TIMEOUT.toSeconds(), TimeUnit.SECONDS);
        } catch (TimeoutException e) {
            future.cancel(true);
            throw e;
        }
    }

    private String writeCandidateJson(List<FixedExpenseCoachingCandidate> candidates) {
        try {
            List<Map<String, Object>> payload = candidates.stream()
                    .map(candidate -> Map.<String, Object>of(
                            "targetRef", candidate.targetRef(),
                            "name", candidate.name(),
                            "categoryName", candidate.categoryName(),
                            "monthlyAmount", candidate.monthlyAmount(),
                            "annualAmount", candidate.annualAmount()))
                    .toList();
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("고정지출 코칭 후보 직렬화에 실패했습니다.", e);
        }
    }
}
