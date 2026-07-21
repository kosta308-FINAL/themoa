package com.weaone.themoa.domain.financialsearch.service;

import com.weaone.themoa.domain.financialsearch.config.FinancialRagProperties;
import com.weaone.themoa.domain.financialsearch.dto.FinancialSearchRequest;
import com.weaone.themoa.domain.financialsearch.dto.FinancialSearchResponse;
import com.weaone.themoa.domain.financialsearch.dto.FinancialSearchResultItem;
import com.weaone.themoa.domain.financialsearch.dto.FinancialSortOption;
import com.weaone.themoa.domain.financialsearch.repository.FinancialLoanSearchRepository;
import com.weaone.themoa.domain.financialsearch.repository.FinancialSavingsSearchRepository;
import com.weaone.themoa.domain.recommend.entity.LoanProduct;
import com.weaone.themoa.domain.recommend.entity.LoanProductOption;
import com.weaone.themoa.domain.recommend.entity.SavingsProduct;
import com.weaone.themoa.domain.recommend.entity.SavingsProductOption;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

// 금융상품 단순 조회형 검색의 핵심 서비스.
// 검색 흐름: 하이브리드 검색(벡터 유사도 + SQL 키워드 매칭을 합산한 점수로 랭킹) -> 그래도 0건이면
// LLM이 검색어를 확장해 재시도. 벡터만 쓰면 "청년"과 "육아"처럼 서로 다른 개념이 문서상 자주 같이
// 언급돼 혼동되는 문제가 있어서, 실제로 검색어가 포함돼 있는지(키워드 점수)도 같이 반영한다.
// (1단계 이식: app.financial.rag.enabled=false면 semanticScores가 빈 맵을 반환해 순수 LIKE 검색으로 동작하고,
//  ChatClient 빈이 없으면 LLM 확장/설명도 자동 skip된다.)
@Service
public class FinancialProductSearchService {

    // 최종 점수 = 의미유사도 * SEMANTIC_WEIGHT + 키워드일치 * LEXICAL_WEIGHT
    // 키워드 일치가 더 확실한 신호라고 판단해 의미유사도보다 비중을 더 준다.
    private static final double SEMANTIC_WEIGHT = 0.4;
    private static final double LEXICAL_WEIGHT = 0.6;

    // 특정 인구집단을 겨냥한 상품인지 룰 기반으로 감지하기 위한 키워드 그룹.
    // 검색어가 어느 그룹에 속하는지 감지되면, 상품 텍스트가 "다른 그룹"에만 해당하고 검색어 그룹엔
    // 전혀 해당하지 않을 경우 유사도 점수와 무관하게 결과에서 제외한다(하드필터).
    // 예: "청년"으로 검색했는데 상품이 "임산부/육아" 전용이고 "청년"이란 언급이 전혀 없으면 제외.
    private static final Map<String, List<String>> DEMOGRAPHIC_GROUPS = Map.ofEntries(
            Map.entry("YOUTH", List.of("청년", "MZ", "사회초년생", "청소년", "대학생")),
            Map.entry("CHILDCARE", List.of("임산부", "임신", "출산", "육아", "다자녀", "아이사랑", "자녀", "키즈", "꿈나무", "아동")),
            Map.entry("DISABILITY", List.of("장애인", "장애우", "장애")),
            Map.entry("SENIOR", List.of("시니어", "고령자", "실버", "어르신", "노인")),
            Map.entry("MULTICULTURAL", List.of("다문화", "외국인")),
            Map.entry("VETERAN", List.of("국가유공자", "보훈", "상이군경")),
            Map.entry("LOW_INCOME", List.of("기초생활수급자", "차상위", "서민", "저소득")),
            Map.entry("FARMER_FISHER", List.of("농업인", "어업인", "농어민", "귀농", "귀어")),
            Map.entry("NEWLYWED", List.of("신혼부부", "예비부부"))
    );

    // 검색어가 "대출"을 원하는지 "예금/적금(저축)"을 원하는지 감지하는 키워드.
    // 둘 다 걸리면(예: "돈 없을 때 돈 불리기"처럼 대출 트리거와 저축 트리거가 같이 있는 경우) 저축 쪽을 우선한다.
    private static final List<String> LOAN_INTENT_KEYWORDS =
            List.of("대출", "빌리", "빌려", "급전", "돈이 없", "돈 없");
    private static final List<String> SAVINGS_INTENT_KEYWORDS =
            List.of("적금", "예금", "저축", "모으", "불리");

    // 결과 화면에 한 번에 보여줄 최대 개수(topK)만큼만 LLM에 설명을 부탁한다. 그 이상은 토큰 낭비.
    private final FinancialSavingsSearchRepository savingsProductRepository;
    private final FinancialLoanSearchRepository loanProductRepository;
    private final ObjectProvider<ChatClient.Builder> chatClientBuilderProvider;
    // 정책 VectorStore가 아니라 금융 전용 컬렉션 래퍼를 주입받는다(정책 빈과 타입이 달라 충돌 없음).
    // 빈이 없으면(금융 RAG 꺼짐/Qdrant 미가동) getIfAvailable()이 null → semanticScores가 빈 맵 반환 → LIKE 폴백.
    private final ObjectProvider<FinancialVectorStore> financialVectorStoreProvider;
    private final FinancialRagProperties ragProperties;
    private final BankUrlResolver bankUrlResolver;
    private final ObjectMapper objectMapper;

    public FinancialProductSearchService(FinancialSavingsSearchRepository savingsProductRepository,
                                         FinancialLoanSearchRepository loanProductRepository,
                                         ObjectProvider<ChatClient.Builder> chatClientBuilderProvider,
                                         ObjectProvider<FinancialVectorStore> financialVectorStoreProvider,
                                         FinancialRagProperties ragProperties,
                                         BankUrlResolver bankUrlResolver,
                                         ObjectMapper objectMapper) {
        this.savingsProductRepository = savingsProductRepository;
        this.loanProductRepository = loanProductRepository;
        this.chatClientBuilderProvider = chatClientBuilderProvider;
        this.financialVectorStoreProvider = financialVectorStoreProvider;
        this.ragProperties = ragProperties;
        this.bankUrlResolver = bankUrlResolver;
        this.objectMapper = objectMapper;
    }

    // readOnly 트랜잭션이 필요한 이유: vectorSearch()에서 찾은 상품을 SavingsProduct.getOptions()로 조회할 때
    // LAZY 컬렉션이라 트랜잭션(영속성 컨텍스트)이 열려있어야 접근 가능하다.
    // 연금저축(annuitySavingProductsSearch)은 finlife 서버측 장애로 데이터 수집 자체가 안 돼서
    // pension_product 테이블이 0건이다. 검색어에 "연금"이 있으면 엉뚱한 예금/적금이 우연히 걸려서
    // 나오는 대신, 검색을 아예 안 돌리고 이유를 명확히 안내한다.
    private static final String PENSION_UNAVAILABLE_MESSAGE =
            "연금 상품은 현재 저희 쪽에서 데이터를 제공받는 API(finlife)에 장애가 있어 검색할 수 없습니다. API가 복구되면 제공할 예정입니다.";

    @Transactional(readOnly = true)
    public FinancialSearchResponse search(FinancialSearchRequest request) {
        String query = request.query().trim();
        if (query.contains("연금")) {
            return new FinancialSearchResponse(query, request.sort().name(), 0, List.of(), PENSION_UNAVAILABLE_MESSAGE, List.of());
        }
        List<FinancialSearchResultItem> results = hybridSearch(query);

        String message = null;
        List<String> suggestedQueries = List.of();
        if (results.isEmpty()) {
            // 하이브리드 검색(벡터+키워드)도 0건일 때만 LLM 호출(비용 절약).
            String expandedKeyword = expandQueryWithLlm(query);
            if (StringUtils.hasText(expandedKeyword)) {
                results = hybridSearch(expandedKeyword);
                if (!results.isEmpty()) {
                    message = "\"" + query + "\"에 대한 결과가 없어 \"" + expandedKeyword + "\"(으)로 확장하여 검색했습니다.";
                }
            }
            if (results.isEmpty()) {
                // 확장 재시도까지 실패하면, 대안 검색어 후보를 사용자에게 다시 묻지 않고 서버가
                // 직접 하나씩 돌려봐서 결과가 있는 첫 번째 것으로 자동 대체한다("노후" -> "시니어적금").
                List<String> alternatives = suggestAlternativeQueries(query);
                String matchedAlternative = null;
                for (String alternative : alternatives) {
                    List<FinancialSearchResultItem> alternativeResults = hybridSearch(alternative);
                    if (!alternativeResults.isEmpty()) {
                        results = alternativeResults;
                        matchedAlternative = alternative;
                        break;
                    }
                }
                if (!results.isEmpty()) {
                    message = "\"" + query + "\"에 대한 결과가 없어 \"" + matchedAlternative + "\"(으)로 확장하여 검색했습니다.";
                } else {
                    // 대안 검색어까지 전부 결과가 없으면 진짜로 결과가 없는 것 -> 칩으로만 제안.
                    suggestedQueries = alternatives;
                    message = "검색 조건에 맞는 금융상품을 찾지 못했습니다."
                            + (suggestedQueries.isEmpty() ? " 다른 검색어로 다시 시도해 보세요." : " 아래 검색어로 다시 시도해 보세요.");
                }
            }
        }

        FinancialSortOption effectiveSort = resolveEffectiveSort(query, request.sort());
        List<FinancialSearchResultItem> sorted = sort(results, effectiveSort);
        // AI가 상품 조건을 직접 보고 이유를 작성한다. 실패/비활성 시 기존 규칙 기반 이유가 그대로 남는다.
        sorted = explainWithLlm(query, sorted);
        return new FinancialSearchResponse(query, effectiveSort.name(), sorted.size(), sorted, message, suggestedQueries);
    }

    private static final List<String> HIGH_RATE_INTENT_KEYWORDS =
            List.of("고금리", "높은 금리", "높은금리", "최고 금리", "최고금리", "이자 많이", "이자많이");

    // 사용자가 정렬을 직접 고르지 않았는데(RELEVANCE, 기본값) 검색어에 "고금리" 같은 정량적 의도가
    // 담겨 있으면 관련도순 대신 금리 높은순으로 자동 정렬한다. 사용자가 정렬을 명시로 고른 경우엔 존중한다.
    private FinancialSortOption resolveEffectiveSort(String query, FinancialSortOption requestedSort) {
        if (requestedSort != FinancialSortOption.RELEVANCE) {
            return requestedSort;
        }
        for (String keyword : HIGH_RATE_INTENT_KEYWORDS) {
            if (query.contains(keyword)) {
                return FinancialSortOption.RATE_DESC;
            }
        }
        return requestedSort;
    }

    // 벡터 유사도 후보와 SQL 키워드 매칭 후보를 합쳐서 점수를 매기고, 점수 높은 순으로 topK개만 남긴다.
    // 벡터 검색만 쓰면 "청년"과 "육아"처럼 문서상 자주 같이 언급되는 단어를 혼동하는데,
    // 실제 키워드 포함 여부를 같이 반영하면 그런 오탐이 줄어든다.
    private List<FinancialSearchResultItem> hybridSearch(String query) {
        Map<String, Double> semanticScores = semanticScores(query);
        int termCount = Math.max(1, query.trim().split("\\s+").length);
        Map<String, Set<String>> lexicalMatches = lexicalMatches(query);
        Map<String, Double> lexicalScores = new LinkedHashMap<>();
        lexicalMatches.forEach((key, terms) -> lexicalScores.put(key, terms.size() / (double) termCount));
        Set<String> queryGroups = detectGroups(query);
        String typeIntent = detectProductTypeIntent(query);
        Integer queryAge = extractAge(query);

        // 규칙 기반(키워드/정규식)으로 상품유형·인구집단을 하나도 못 잡아냈거나, "살"/"세"라는 글자는
        // 있는데 숫자+세/살 정규식으로 나이를 못 뽑아낸 경우("서른네살"처럼 한글 숫자로 표현된 경우)에만
        // LLM에게 검색어 해석을 맡긴다(항상 부르면 매 검색이 느려지므로 최후 보강 수단으로만 사용).
        boolean weakTypeOrGroupSignal = typeIntent == null && queryGroups.isEmpty();
        boolean missedTextualAge = queryAge == null && (query.contains("살") || query.contains("세"));
        if (weakTypeOrGroupSignal || missedTextualAge) {
            QueryIntentHint hint = interpretQueryWithLlm(query);
            if (hint != null) {
                if (hint.productType() != null) {
                    typeIntent = hint.productType();
                }
                if (hint.age() != null) {
                    queryAge = hint.age();
                }
                if (hint.populationGroup() != null && DEMOGRAPHIC_GROUPS.containsKey(hint.populationGroup())) {
                    queryGroups = Set.of(hint.populationGroup());
                }
            }
        }

        LinkedHashSet<String> candidateKeys = new LinkedHashSet<>(semanticScores.keySet());
        candidateKeys.addAll(lexicalMatches.keySet());
        if (candidateKeys.isEmpty()) {
            // 검색어 자체가 후보를 하나도 못 찾았어도, "대출"/"저축" 의도는 감지된 경우
            // (예: "급전", "돈 빌리기"처럼 상품 데이터엔 없는 구어체 표현) 완전히 빈 결과 대신
            // 해당 유형의 상품군을 최소한 보여준다.
            return typedFallback(typeIntent);
        }

        Set<Long> savingsIds = new LinkedHashSet<>();
        Set<Long> loanIds = new LinkedHashSet<>();
        for (String key : candidateKeys) {
            String[] parts = key.split("-", 2);
            Long id = Long.valueOf(parts[1]);
            if ("SAVINGS".equals(parts[0])) {
                savingsIds.add(id);
            } else {
                loanIds.add(id);
            }
        }

        List<ScoredItem> scoredItems = new ArrayList<>();
        if (!savingsIds.isEmpty() && !"LOAN".equals(typeIntent)) {
            for (SavingsProduct product : savingsProductRepository.findAllById(savingsIds)) {
                String productText = nullToEmpty(product.getJoinTarget()) + " " + nullToEmpty(product.getSpecialCondition());
                if (isDemographicMismatch(queryGroups, productText)) {
                    continue;
                }
                if (isAgeMismatch(queryAge, product.getMinAge(), product.getMaxAge())) {
                    continue;
                }
                String key = "SAVINGS-" + product.getId();
                Set<String> matchedTerms = lexicalMatches.getOrDefault(key, Set.of());
                String reason = matchReason(matchedTerms, semanticScores.containsKey(key));
                scoredItems.add(new ScoredItem(toItem(product, reason), score(key, semanticScores, lexicalScores)));
            }
        }
        if (!loanIds.isEmpty() && !"SAVINGS".equals(typeIntent)) {
            for (LoanProduct product : loanProductRepository.findAllById(loanIds)) {
                String productText = nullToEmpty(product.getSpecialCondition());
                if (isDemographicMismatch(queryGroups, productText)) {
                    continue;
                }
                String key = "LOAN-" + product.getId();
                Set<String> matchedTerms = lexicalMatches.getOrDefault(key, Set.of());
                String reason = matchReason(matchedTerms, semanticScores.containsKey(key));
                scoredItems.add(new ScoredItem(toItem(product, reason), score(key, semanticScores, lexicalScores)));
            }
        }

        if (scoredItems.isEmpty()) {
            // 후보는 있었지만 전부 다른 유형(대출/저축)이었거나 인구집단 하드필터에 걸려 다 빠진 경우도 동일하게 처리.
            return typedFallback(typeIntent);
        }

        return scoredItems.stream()
                .sorted(Comparator.comparingDouble(ScoredItem::score).reversed())
                .limit(ragProperties.getTopK())
                .map(ScoredItem::item)
                .toList();
    }

    // 검색어와 정확히 일치하는 상품이 없어도, "대출"/"저축" 의도만은 확실할 때 그 유형의 상품군을
    // 최소한 보여주는 폴백. 진짜 관련성 신호가 없으므로 대표금리 순으로만 정렬해 보여준다.
    private List<FinancialSearchResultItem> typedFallback(String typeIntent) {
        String reason = "정확히 일치하는 상품 정보는 없지만, 검색어가 %s 관련 표현으로 보여 관련 상품을 보여드려요";
        if ("LOAN".equals(typeIntent)) {
            return loanProductRepository.findAll().stream()
                    .map(product -> toItem(product, reason.formatted("대출")))
                    .sorted(Comparator.comparing(FinancialSearchResultItem::representativeRate,
                            Comparator.nullsLast(Comparator.naturalOrder())))
                    .limit(ragProperties.getTopK())
                    .toList();
        }
        if ("SAVINGS".equals(typeIntent)) {
            return savingsProductRepository.findAll().stream()
                    .map(product -> toItem(product, reason.formatted("예금/적금")))
                    .sorted(Comparator.comparing(FinancialSearchResultItem::representativeRate,
                            Comparator.nullsLast(Comparator.reverseOrder())))
                    .limit(ragProperties.getTopK())
                    .toList();
        }
        return List.of();
    }

    // 사람이 읽을 수 있는 "이 상품이 왜 나왔는지" 설명을 규칙 기반으로 만든다(LLM 미사용, 빠르고 무료).
    private String matchReason(Set<String> matchedTerms, boolean hasSemanticMatch) {
        List<String> reasons = new ArrayList<>();
        if (!matchedTerms.isEmpty()) {
            reasons.add("검색어 '" + String.join(", ", matchedTerms) + "'가 상품 정보에 포함돼 있어요");
        }
        if (hasSemanticMatch) {
            reasons.add("검색어와 의미상 유사한 상품이에요");
        }
        if (reasons.isEmpty()) {
            return "관련 상품으로 판단됐어요";
        }
        return String.join(". ", reasons);
    }

    // 검색어가 특정 인구집단(예: 청년)을 겨냥한 게 감지됐는데, 상품 텍스트는 "다른" 인구집단
    // (예: 육아) 키워드만 있고 검색어 집단 키워드는 전혀 없다면 하드필터로 제외한다.
    // 검색어에 인구집단 키워드가 없거나(일반 검색), 상품 텍스트에 아무 그룹도 안 걸리면(일반 상품) 필터링하지 않는다.
    private boolean isDemographicMismatch(Set<String> queryGroups, String productText) {
        if (queryGroups.isEmpty()) {
            return false;
        }
        Set<String> productGroups = detectGroups(productText);
        return !productGroups.isEmpty() && Collections.disjoint(productGroups, queryGroups);
    }

    private static final Pattern AGE_PATTERN = Pattern.compile("(\\d{1,3})\\s*(세|살)");

    // 검색어에 "34세"/"34살"처럼 나이가 포함돼 있으면 숫자로 뽑아낸다. 1~120 범위를 벗어나면
    // 상품코드에 우연히 섞인 숫자(예: "IBK D-day적금") 오인식을 막기 위해 무시한다.
    private Integer extractAge(String query) {
        Matcher matcher = AGE_PATTERN.matcher(query);
        if (!matcher.find()) {
            return null;
        }
        int age = Integer.parseInt(matcher.group(1));
        return (age >= 1 && age <= 120) ? age : null;
    }

    // 검색어에 나이가 명시돼 있고, 상품에 실제로 min_age/max_age 범위가 설정돼 있는데 그 나이가
    // 범위를 벗어나면 제외한다. 상품에 범위가 아예 없으면(둘 다 null) 나이 제한이 없다는 뜻이라 통과시킨다.
    private boolean isAgeMismatch(Integer queryAge, Integer minAge, Integer maxAge) {
        if (queryAge == null) {
            return false;
        }
        if (minAge != null && queryAge < minAge) {
            return true;
        }
        return maxAge != null && queryAge > maxAge;
    }

    // 검색어에 저축 관련 단어가 있으면 무조건 "SAVINGS"(저축 트리거가 대출 트리거보다 우선).
    // 저축 단어가 없고 대출 단어만 있으면 "LOAN". 둘 다 없으면 null(필터 없이 예금/적금+대출 다 보여줌).
    private String detectProductTypeIntent(String query) {
        boolean hasSavingsIntent = SAVINGS_INTENT_KEYWORDS.stream().anyMatch(query::contains);
        if (hasSavingsIntent) {
            return "SAVINGS";
        }
        boolean hasLoanIntent = LOAN_INTENT_KEYWORDS.stream().anyMatch(query::contains);
        return hasLoanIntent ? "LOAN" : null;
    }

    private Set<String> detectGroups(String text) {
        if (!StringUtils.hasText(text)) {
            return Set.of();
        }
        Set<String> groups = new LinkedHashSet<>();
        DEMOGRAPHIC_GROUPS.forEach((group, keywords) -> {
            if (keywords.stream().anyMatch(text::contains)) {
                groups.add(group);
            }
        });
        return groups;
    }

    private String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    private double score(String key, Map<String, Double> semanticScores, Map<String, Double> lexicalScores) {
        double semantic = semanticScores.getOrDefault(key, 0.0);
        double lexical = lexicalScores.getOrDefault(key, 0.0);
        return semantic * SEMANTIC_WEIGHT + lexical * LEXICAL_WEIGHT;
    }

    private record ScoredItem(FinancialSearchResultItem item, double score) {
    }

    // Qdrant 벡터 검색 결과를 "SAVINGS-1" 같은 키 -> 유사도 점수 맵으로 변환한다.
    // 여기서 쓰는 minimumSimilarity가 실제로 적용되는 임계값이다(하이브리드 방식이라
    // 정확도는 이후 키워드 점수와 합산하는 단계에서 한 번 더 걸러진다).
    // 1단계 이식: ragProperties.isEnabled()가 false면 여기서 바로 빈 맵을 반환해 벡터스토어(정책 Qdrant 컬렉션)를
    // 아예 건드리지 않는다.
    private Map<String, Double> semanticScores(String query) {
        if (!ragProperties.isEnabled()) {
            return Map.of();
        }
        FinancialVectorStore financialVectorStore = financialVectorStoreProvider.getIfAvailable();
        if (financialVectorStore == null) {
            return Map.of();
        }
        VectorStore vectorStore = financialVectorStore.delegate();
        List<Document> documents = vectorStore.similaritySearch(SearchRequest.builder()
                .query(query)
                .topK(ragProperties.getRetryTopK())
                .similarityThreshold(ragProperties.getMinimumSimilarity())
                .build());
        Map<String, Double> scores = new LinkedHashMap<>();
        for (Document document : documents) {
            Object idValue = document.getMetadata().get("financialProductId");
            Object kindValue = document.getMetadata().get("productKind");
            // Qdrant에서 돌아오는 metadata 값이 항상 숫자 타입인 건 아니라(문자열로 오는 경우 있음) 방어적으로 파싱한다.
            Long id = toLong(idValue);
            if (id == null || kindValue == null) {
                continue;
            }
            String key = kindValue + "-" + id;
            double score = document.getScore() == null ? 0.0 : document.getScore();
            scores.put(key, score);
        }
        return scores;
    }

    private Long toLong(Object value) {
        if (value instanceof Number number) {
            return number.longValue();
        }
        if (value instanceof String text) {
            try {
                return Long.parseLong(text.trim());
            } catch (NumberFormatException ex) {
                return null;
            }
        }
        return null;
    }

    // 검색어를 단어 단위로 쪼개서 각 단어를 SQL LIKE로 찾고, 상품별로 "어떤 단어가 일치했는지" 모아둔다.
    // 문구 전체를 통으로 찾으면("청년 적금") 정확히 그 문구가 붙어있는 상품만 걸려서 키워드 신호가 거의
    // 안 잡히기 때문에, 단어별로 나눠서 매칭 범위를 넓힌 것. 일치한 단어 목록은 matchReason에도 쓰인다.
    private Map<String, Set<String>> lexicalMatches(String query) {
        List<String> terms = expandWithDemographicSynonyms(query.trim().split("\\s+"));
        Map<String, Set<String>> matches = new LinkedHashMap<>();
        for (String term : terms) {
            if (!StringUtils.hasText(term)) {
                continue;
            }
            for (SavingsProduct product : savingsProductRepository.searchByKeyword(term)) {
                matches.computeIfAbsent("SAVINGS-" + product.getId(), k -> new LinkedHashSet<>()).add(term);
            }
            for (LoanProduct product : loanProductRepository.searchByKeyword(term)) {
                matches.computeIfAbsent("LOAN-" + product.getId(), k -> new LinkedHashSet<>()).add(term);
            }
        }
        return matches;
    }

    // 검색어 단어가 인구집단 그룹(DEMOGRAPHIC_GROUPS)의 동의어 중 하나면, 그 그룹의 나머지 동의어도
    // 같이 검색 대상에 넣는다. 예: "임산부"로 검색해도 상품 텍스트엔 "임신"으로만 적혀 있는 경우가 많아서
    // (실측: DB에 "임산부" 0건, "임신" 5건), 원래 단어만으론 후보가 아예 안 잡히는 문제가 있었다.
    private List<String> expandWithDemographicSynonyms(String[] originalTerms) {
        Set<String> expanded = new LinkedHashSet<>();
        for (String term : originalTerms) {
            if (!StringUtils.hasText(term)) {
                continue;
            }
            expanded.add(term);
            for (List<String> synonyms : DEMOGRAPHIC_GROUPS.values()) {
                if (synonyms.contains(term)) {
                    expanded.addAll(synonyms);
                }
            }
        }
        return List.copyOf(expanded);
    }

    private FinancialSearchResultItem toItem(SavingsProduct product, String matchReason) {
        BigDecimal bestRate = product.getOptions().stream()
                .map(SavingsProductOption::getMaxRate)
                .filter(rate -> rate != null)
                .max(Comparator.naturalOrder())
                .orElse(null);
        Integer shortestTerm = product.getOptions().stream()
                .map(SavingsProductOption::getTermMonth)
                .filter(term -> term != null)
                .min(Comparator.naturalOrder())
                .orElse(null);
        // recommend 엔티티의 productType은 SavingsType enum이라, 검색 응답 DTO의 String 필드에 맞춰 name()으로 변환한다.
        return new FinancialSearchResultItem(product.getId(), product.getProductType().name(), product.getCompanyName(),
                product.getProductName(), product.getJoinMethod(), bestRate, shortestTerm, product.getSpecialCondition(),
                matchReason, bankUrlResolver.resolve(product.getCompanyName()));
    }

    private FinancialSearchResultItem toItem(LoanProduct product, String matchReason) {
        BigDecimal minRate = product.getOptions().stream()
                .map(LoanProductOption::getRateMin)
                .filter(rate -> rate != null)
                .min(Comparator.naturalOrder())
                .orElse(null);
        // recommend 엔티티의 productType은 LoanType enum이라, 검색 응답 DTO의 String 필드에 맞춰 name()으로 변환한다.
        return new FinancialSearchResultItem(product.getId(), product.getProductType().name(), product.getCompanyName(),
                product.getProductName(), product.getJoinMethod(), minRate, null, product.getSpecialCondition(),
                matchReason, bankUrlResolver.resolve(product.getCompanyName()));
    }

    // RELEVANCE는 vectorSearch/searchByKeyword가 이미 정해준 순서를 그대로 쓴다(재정렬 없음).
    private List<FinancialSearchResultItem> sort(List<FinancialSearchResultItem> results, FinancialSortOption sort) {
        Comparator<FinancialSearchResultItem> comparator = switch (sort) {
            case RATE_DESC -> Comparator.comparing(FinancialSearchResultItem::representativeRate,
                    Comparator.nullsLast(Comparator.reverseOrder()));
            case RATE_ASC -> Comparator.comparing(FinancialSearchResultItem::representativeRate,
                    Comparator.nullsLast(Comparator.naturalOrder()));
            case TERM_ASC -> Comparator.comparing(FinancialSearchResultItem::representativeTermMonth,
                    Comparator.nullsLast(Comparator.naturalOrder()));
            case RELEVANCE -> null;
        };
        if (comparator == null) {
            return results;
        }
        return results.stream().sorted(comparator).toList();
    }

    // OpenAI 채팅모델이 꺼져있으면(spring.ai.model.chat=none) builder 자체를 못 가져와 예외가 나서
    // try-catch로 감싸 실패해도 검색 자체는 계속 진행되게(null 반환) 한다.
    // LLM이 자유 문장 검색어에서 뽑아낸 상품유형/나이/인구집단 힌트. 셋 다 못 뽑아냈으면 null로 둔다.
    private record QueryIntentHint(String productType, Integer age, String populationGroup) {
    }

    // 규칙 기반(키워드/정규식)으로 상품유형·나이·인구집단을 전혀 못 잡아낸 자유 문장 검색어를 LLM에게
    // 해석시킨다(예: "서른네살인데 목돈 모으고 싶어" -> productType=SAVINGS, age=34). 실패하거나
    // 파싱이 안 되면 null을 반환해 규칙 기반 결과만 그대로 쓰게 한다.
    private QueryIntentHint interpretQueryWithLlm(String query) {
        try {
            ChatClient.Builder builder = chatClientBuilderProvider.getIfAvailable();
            if (builder == null) {
                return null;
            }
            String groupKeys = String.join("|", DEMOGRAPHIC_GROUPS.keySet());
            String prompt = """
                    사용자가 금융상품(예금/적금/대출) 검색창에 아래처럼 정형화된 단어 없이 말로 풀어서
                    검색어를 입력했습니다. 이 문장에서 아래 3가지 정보를 추론해서 JSON 한 줄로만 출력하세요.
                    다른 설명은 절대 넣지 마세요.
                    - productType: "SAVINGS"(예금/적금을 원함), "LOAN"(대출을 원함) 중 확실히 판단되면 그 값, 아니면 null
                    - age: 문장에 나이가 나오거나("서른네살", "30대 초반" 등) 유추 가능하면 정수(만 나이), 아니면 null
                    - populationGroup: 다음 중 하나에 해당하면 그 값, 아니면 null: %s
                    출력 형식 예시: {"productType":"SAVINGS","age":34,"populationGroup":null}
                    검색어: %s""".formatted(groupKeys, query);
            String content = builder.build().prompt().user(prompt).call().content();
            if (!StringUtils.hasText(content)) {
                return null;
            }
            return objectMapper.readValue(stripJsonFence(content), QueryIntentHint.class);
        } catch (Exception ex) {
            return null;
        }
    }

    private String expandQueryWithLlm(String query) {
        try {
            ChatClient.Builder builder = chatClientBuilderProvider.getIfAvailable();
            if (builder == null) {
                return null;
            }
            String prompt = """
                    다음은 사용자가 입력한 금융상품(예금/적금/대출) 검색어입니다.
                    이 검색어와 의미가 비슷한, 실제 금융상품명에 쓰일 법한 대체 키워드 1개만 출력하세요.
                    다른 설명 없이 키워드만 출력하세요.
                    검색어: """ + query;
            String content = builder.build().prompt().user(prompt).call().content();
            return content == null ? null : content.trim();
        } catch (Exception ex) {
            return null;
        }
    }

    // 검색어 확장으로도 결과를 못 찾았을 때, 사용자가 시도해볼 만한 대안 검색어를 몇 개 제안한다.
    // OpenAI가 꺼져있거나 실패하면 빈 목록을 반환하고(예외로 검색 자체를 막지 않음), 화면에서는
    // "다른 검색어로 시도해 보세요"라는 일반 메시지만 보여주게 된다.
    private List<String> suggestAlternativeQueries(String query) {
        try {
            ChatClient.Builder builder = chatClientBuilderProvider.getIfAvailable();
            if (builder == null) {
                return List.of();
            }
            String prompt = """
                    사용자가 금융상품(예금/적금/대출) 검색창에 아래 검색어를 입력했는데 결과를 찾지 못했습니다.
                    이 사용자가 대신 시도해볼 만한, 실제 금융상품 검색에 쓰일 법한 대안 검색어를 3개 제안하세요.
                    설명 없이 검색어만 쉼표(,)로 구분해서 한 줄로 출력하세요.
                    검색어: """ + query;
            String content = builder.build().prompt().user(prompt).call().content();
            if (!StringUtils.hasText(content)) {
                return List.of();
            }
            return List.of(content.split(",")).stream()
                    .map(String::trim)
                    .filter(StringUtils::hasText)
                    .limit(3)
                    .toList();
        } catch (Exception ex) {
            return List.of();
        }
    }

    // 결과 목록 전체를 한 번에 LLM에 보내서 상품별 추천 이유를 받아온다(항목마다 따로 호출하면 느리고
    // 비용도 커서, 요청 1번으로 묶어서 처리). 실패하거나 개수가 안 맞으면 기존 규칙 기반 이유를 그대로 둔다.
    private List<FinancialSearchResultItem> explainWithLlm(String query, List<FinancialSearchResultItem> items) {
        if (items.isEmpty()) {
            return items;
        }
        try {
            ChatClient.Builder builder = chatClientBuilderProvider.getIfAvailable();
            if (builder == null) {
                return items;
            }
            StringBuilder productList = new StringBuilder();
            for (int i = 0; i < items.size(); i++) {
                FinancialSearchResultItem item = items.get(i);
                String condition = item.specialCondition() == null ? "" : item.specialCondition();
                if (condition.length() > 120) {
                    condition = condition.substring(0, 120);
                }
                productList.append(i + 1).append(". ")
                        .append(item.productName()).append(" / ").append(item.companyName())
                        .append(" / 대표금리 ").append(item.representativeRate()).append("%")
                        .append(" / 조건: ").append(condition)
                        .append('\n');
            }
            String prompt = """
                    사용자가 금융상품을 검색했습니다. 검색어와 아래 상품 목록을 보고, 각 상품이 왜 이 검색어에
                    적합한지(또는 어떤 점이 유용한지) 상품 조건을 근거로 1문장씩 한국어로 설명하세요.
                    ⚠️ 반드시 아래 "조건" 텍스트에 실제로 적힌 내용만 근거로 쓰세요. 조건에 없는 내용(예: 특정
                    대상자·직업·연령층 전용이라거나 소득 증빙이 필요 없다는 식의 자격 조건)은 절대로 지어내지
                    마세요. 조건 텍스트에 검색어와 직접 관련된 근거가 없으면, 그 대상에 특화됐다고 단정하지 말고
                    금리·이용 편의성 등 확인 가능한 사실만으로 일반적인 설명을 쓰세요.
                    반드시 상품 목록과 같은 개수, 같은 순서로 문자열 배열만 JSON으로 출력하세요. 다른 텍스트는 넣지 마세요.
                    검색어: %s
                    상품 목록:
                    %s""".formatted(query, productList);
            String content = builder.build().prompt().user(prompt).call().content();
            String[] reasons = objectMapper.readValue(stripJsonFence(content), String[].class);
            if (reasons.length != items.size()) {
                return items;
            }
            List<FinancialSearchResultItem> withReasons = new ArrayList<>(items.size());
            for (int i = 0; i < items.size(); i++) {
                String reason = StringUtils.hasText(reasons[i]) ? reasons[i].trim() : items.get(i).matchReason();
                withReasons.add(items.get(i).withMatchReason(reason));
            }
            return withReasons;
        } catch (Exception ex) {
            return items;
        }
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
