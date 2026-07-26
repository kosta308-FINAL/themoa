package com.weaone.themoa.domain.customerservice.service;

import com.weaone.themoa.domain.customerservice.dto.request.CustomerServiceChatRequest;
import com.weaone.themoa.domain.customerservice.dto.response.CustomerServiceChatResponse;
import com.weaone.themoa.domain.customerservice.entity.CustomerServiceUnansweredQuestion;
import com.weaone.themoa.domain.customerservice.entity.UnansweredQuestionReason;
import com.weaone.themoa.domain.customerservice.rag.CustomerKnowledgeCitation;
import com.weaone.themoa.domain.customerservice.rag.CustomerKnowledgeDocument;
import com.weaone.themoa.domain.customerservice.rag.CustomerKnowledgeRetriever;
import com.weaone.themoa.domain.customerservice.rag.CustomerKnowledgeSearchResult;
import com.weaone.themoa.domain.customerservice.rag.CustomerServiceRagSettingValues;
import com.weaone.themoa.domain.customerservice.repository.CustomerServiceUnansweredQuestionRepository;
import com.weaone.themoa.domain.member.repository.MemberRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

@Slf4j
@Service
public class CustomerServiceChatService {

    private static final String HUMAN_SUPPORT_MARKER = "NEEDS_HUMAN_SUPPORT:";

    public static final String DEFAULT_SYSTEM_PROMPT = """
            당신은 더모아 고객센터 전용 챗봇이다.
            반드시 제공된 고객센터 지식 문서에 근거해서만 답변한다.
            답변 전에 먼저 아래 "고객센터 지식 문서"가 사용자 질문에 실제로 답할 수 있는 내용인지 스스로 검증한다.
            문서가 질문과 주제가 다르거나, 질문에서 묻는 조건·수치·절차를 문서가 직접 언급하지 않거나,
            문서 내용만으로는 애매하여 빈 부분을 추측으로 채워야 하는 경우에는 답변을 만들어내지 않는다.
            이 경우 "제공된 문서만으로는 정확히 답변드리기 어렵다"는 취지로 안내하고 1:1 문의 접수를 안내한다.
            문서 일부가 관련 있어도 질문의 핵심(예: 정확한 시간·조건·수치)을 그 문서가 명시하지 않으면 단정적으로 답변하지 않는다.
            더모아 서비스 사용법, FAQ, 카드 연동, 하루 권장 소비액, 고정지출, 수기 지출,
            계정 보안, 청년정책 기능 사용법, 금융상품 추천 기능 사용법만 안내한다.
            실제 청년정책 추천 결과, 금융상품 가입 판단, 개인 소비 데이터 분석, 외부 최신 정보는 생성하지 않는다.
            정책 또는 금융상품의 실제 추천이 필요하면 해당 기능 화면으로 이동하도록 안내한다.
            근거가 부족하거나 개인 계정 확인이 필요한 문제는 추측하지 말고 1:1 문의 접수를 안내한다.
            비밀번호, 카드사 원본 인증정보, 주민등록번호 등 민감정보를 요구하지 않는다.
            고객센터 지식 문서의 정책 사실, 기준 시간, 처리 조건을 우선 반영한다.
            답변은 한국어 Markdown으로 5문장 이내로 간결하게 작성한다.
            """;

    private static final ExecutorService CHAT_EXECUTOR = Executors.newCachedThreadPool(runnable -> {
        Thread thread = new Thread(runnable, "customer-service-chat");
        thread.setDaemon(true);
        return thread;
    });

    private final CustomerKnowledgeRetriever retriever;
    private final CustomerServiceRagSettingService ragSettingService;
    private final ObjectProvider<ChatModel> openAiChatModelProvider;
    private final CustomerServiceUnansweredQuestionRepository unansweredQuestionRepository;
    private final MemberRepository memberRepository;

    public CustomerServiceChatService(CustomerKnowledgeRetriever retriever,
                                      CustomerServiceRagSettingService ragSettingService,
                                      @Qualifier("openAiChatModel") ObjectProvider<ChatModel> openAiChatModelProvider,
                                      CustomerServiceUnansweredQuestionRepository unansweredQuestionRepository,
                                      MemberRepository memberRepository) {
        this.retriever = retriever;
        this.ragSettingService = ragSettingService;
        this.openAiChatModelProvider = openAiChatModelProvider;
        this.unansweredQuestionRepository = unansweredQuestionRepository;
        this.memberRepository = memberRepository;
    }

    @Transactional
    public CustomerServiceChatResponse chat(Long memberId, CustomerServiceChatRequest request) {
        CustomerServiceRagSettingValues settings = ragSettingService.current();
        List<CustomerKnowledgeSearchResult> results = retriever.retrieve(
                request.message(), settings.topK(), settings.minimumSimilarity());
        List<CustomerKnowledgeCitation> citations = citations(results);
        if (results.isEmpty()) {
            String answer = noGroundingAnswer();
            recordUnanswered(memberId, request, UnansweredQuestionReason.NO_GROUNDING, answer);
            return new CustomerServiceChatResponse(request.conversationId(), answer, List.of(), true);
        }
        GroundedChatAnswer answer = generateGroundedChatAnswer(request.message(), results, settings.systemPrompt());
        if (answer.needsHumanSupport()) {
            recordUnanswered(
                    memberId,
                    request,
                    UnansweredQuestionReason.NEEDS_HUMAN_SUPPORT,
                    answer.answerMarkdown());
        }
        return new CustomerServiceChatResponse(
                request.conversationId(), answer.answerMarkdown(), citations, answer.needsHumanSupport());
    }

    private void recordUnanswered(Long memberId, CustomerServiceChatRequest request,
                                  UnansweredQuestionReason reason, String answerMarkdown) {
        unansweredQuestionRepository.save(CustomerServiceUnansweredQuestion.create(
                memberRepository.getReferenceById(memberId),
                request.conversationId(),
                request.message(),
                reason,
                answerMarkdown,
                LocalDateTime.now()));
    }

    public String generateGroundedAnswer(String message, List<CustomerKnowledgeSearchResult> results,
                                         String systemPrompt) {
        return generateGroundedChatAnswer(message, results, systemPrompt).answerMarkdown();
    }

    public GroundedChatAnswer generateGroundedChatAnswer(String message, List<CustomerKnowledgeSearchResult> results,
                                                         String systemPrompt) {
        ChatModel chatModel = openAiChatModelProvider.getIfAvailable();
        if (chatModel == null) {
            return new GroundedChatAnswer(templateAnswer(results.get(0).document()), false);
        }
        try {
            String rawAnswer = callWithTimeout(() -> ChatClient.builder(chatModel).build()
                    .prompt()
                    .system(StringUtils.hasText(systemPrompt) ? systemPrompt : DEFAULT_SYSTEM_PROMPT)
                    .user("""
                            사용자 질문:
                            %s

                            고객센터 지식 문서:
                            %s

                            응답 형식:
                            첫 줄에는 반드시 "NEEDS_HUMAN_SUPPORT: true" 또는 "NEEDS_HUMAN_SUPPORT: false"만 쓴다.
                            고객센터 지식 문서만으로 질문에 답할 수 없을 때만 true다.
                            답변이 가능하지만 추가 안내로 1:1 문의를 언급하는 경우는 false다.
                            둘째 줄부터 사용자에게 보여줄 답변을 쓴다.
                            """.formatted(message, context(results)))
                    .call()
                    .content());
            return parseGroundedChatAnswer(rawAnswer);
        } catch (Exception ex) {
            log.warn("고객센터 챗봇 LLM 호출 실패, 템플릿 답변으로 대체합니다.", ex);
            return new GroundedChatAnswer(templateAnswer(results.get(0).document()), false);
        }
    }

    private GroundedChatAnswer parseGroundedChatAnswer(String rawAnswer) {
        String answer = rawAnswer == null ? "" : rawAnswer.trim();
        String[] lines = answer.split("\\R", 2);
        String firstLine = lines.length == 0 ? "" : lines[0].trim();
        if (firstLine.regionMatches(true, 0, HUMAN_SUPPORT_MARKER, 0, HUMAN_SUPPORT_MARKER.length())) {
            boolean needsHumanSupport = Boolean.parseBoolean(
                    firstLine.substring(HUMAN_SUPPORT_MARKER.length()).trim());
            String answerMarkdown = lines.length > 1 ? lines[1].trim() : "";
            return new GroundedChatAnswer(answerMarkdown, needsHumanSupport);
        }
        return new GroundedChatAnswer(answer, false);
    }

    private String callWithTimeout(Callable<String> call) throws Exception {
        Future<String> future = CHAT_EXECUTOR.submit(call);
        try {
            return future.get(20, TimeUnit.SECONDS);
        } catch (TimeoutException e) {
            future.cancel(true);
            throw e;
        }
    }

    private String context(List<CustomerKnowledgeSearchResult> results) {
        return results.stream()
                .limit(5)
                .map(result -> """
                        [출처: %s / %s / %s]
                        %s
                        """.formatted(
                        result.document().sourceType().name(),
                        result.document().category(),
                        result.document().title(),
                        result.document().content()))
                .collect(Collectors.joining("\n---\n"));
    }

    private List<CustomerKnowledgeCitation> citations(List<CustomerKnowledgeSearchResult> results) {
        return results.stream()
                .limit(5)
                .map(result -> new CustomerKnowledgeCitation(
                        result.document().title(),
                        result.document().sourceType().name(),
                        result.document().sourceId(),
                        result.document().category(),
                        result.score()))
                .toList();
    }

    private String templateAnswer(CustomerKnowledgeDocument document) {
        String content = stripMarkdown(document.content());
        String summary = content.length() > 450 ? content.substring(0, 450) + "..." : content;
        return """
                %s

                더 자세한 개인 계정 확인이 필요한 문제라면 1:1 문의로 이어서 남겨주세요.
                """.formatted(summary).trim();
    }

    private String noGroundingAnswer() {
        return """
                지금 질문은 고객센터 지식 문서에서 충분한 근거를 찾지 못했어요.

                더모아 서비스 사용법, 카드 연동, 하루 권장액, 고정지출, 정책/금융상품 기능 사용법과 관련된 질문으로 다시 입력하거나, 개인 확인이 필요한 문제라면 1:1 문의로 접수해 주세요.
                """;
    }

    private String stripMarkdown(String value) {
        if (!StringUtils.hasText(value)) {
            return "";
        }
        return value.replaceAll("[`*_>#-]", "")
                .replaceAll("\\n{3,}", "\n\n")
                .trim();
    }

    public record GroundedChatAnswer(
            String answerMarkdown,
            boolean needsHumanSupport
    ) {
    }
}
