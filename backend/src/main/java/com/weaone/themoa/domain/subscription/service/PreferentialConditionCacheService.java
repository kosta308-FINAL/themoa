package com.weaone.themoa.domain.subscription.service;

import com.weaone.themoa.domain.financialsearch.service.BankNameFormatter;
import com.weaone.themoa.domain.recommend.repository.SavingsProductRepository;
import com.weaone.themoa.domain.recommend.repository.SavingsProductRepository.ProductConditionSummaryView;
import com.weaone.themoa.domain.subscription.dto.response.ProductConditionSummaryResponse;
import com.weaone.themoa.domain.subscription.entity.PreferentialConditionCache;
import com.weaone.themoa.domain.subscription.entity.PreferentialConditionCache.ParsedItem;
import com.weaone.themoa.domain.subscription.entity.PreferentialConditionCacheItem;
import com.weaone.themoa.domain.subscription.repository.PreferentialConditionCacheRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 상품 우대조건 파싱 결과를 DB에 고정 저장·조회한다.
 *
 * <p>LLM 파싱은 호출마다 결과가 흔들리므로(비결정성), 상품당 한 번 파싱해 저장하고 이후엔 저장값을
 * 읽어 <b>항상 같은 체크리스트</b>를 보장한다. 원문이 바뀌면 배치가 재파싱하되, 관리자가 손본 캐시는
 * 덮지 않고 재검토 표시({@code stale})만 남긴다.
 */
@Service
public class PreferentialConditionCacheService {

    private final PreferentialConditionCacheRepository cacheRepository;
    private final PreferentialConditionLlmParser llmParser;
    private final PreferentialConditionParser regexParser;
    private final SavingsProductRepository savingsProductRepository;
    private final BankNameFormatter bankNameFormatter;

    public PreferentialConditionCacheService(PreferentialConditionCacheRepository cacheRepository,
                                             PreferentialConditionLlmParser llmParser,
                                             PreferentialConditionParser regexParser,
                                             SavingsProductRepository savingsProductRepository,
                                             BankNameFormatter bankNameFormatter) {
        this.cacheRepository = cacheRepository;
        this.llmParser = llmParser;
        this.regexParser = regexParser;
        this.savingsProductRepository = savingsProductRepository;
        this.bankNameFormatter = bankNameFormatter;
    }

    /**
     * 관리자 상품 목록: 우대조건 원문이 있는 판매중 상품을 키워드로 검색하고, 각 상품의 캐시 상태
     * (항목 수·잠금·재검토·캐시 존재)를 함께 담아 돌려준다. 은행명은 표시명으로 변환한다.
     */
    @Transactional(readOnly = true)
    public List<ProductConditionSummaryResponse> searchProducts(String keyword) {
        List<ProductConditionSummaryView> products = savingsProductRepository.searchConditionProducts(keyword);
        List<Long> ids = products.stream().map(ProductConditionSummaryView::getId).toList();
        Map<Long, PreferentialConditionCache> cacheByProductId = ids.isEmpty()
                ? Map.of()
                : cacheRepository.findByProductIdIn(ids).stream()
                        .collect(Collectors.toMap(PreferentialConditionCache::getProductId, Function.identity()));

        return products.stream()
                .map(p -> {
                    PreferentialConditionCache cache = cacheByProductId.get(p.getId());
                    return new ProductConditionSummaryResponse(
                            p.getId(),
                            bankNameFormatter.toDisplayName(p.getCompanyName()),
                            p.getProductName(),
                            p.getProductType() == null ? null : p.getProductType().name(),
                            cache == null ? 0 : cache.getItems().size(),
                            cache != null && cache.isEditedByAdmin(),
                            cache != null && cache.isStale(),
                            cache != null);
                })
                .toList();
    }

    /**
     * 가입 초안용: 캐시가 있으면 그대로 읽고, 없으면(배치 전 신규 상품 등) 지금 파싱해 저장한 뒤 반환한다.
     * 어느 경로든 이후 호출은 같은 값을 돌려준다.
     */
    @Transactional
    public List<PreferentialConditionParser.ParsedCondition> getOrParse(Long productId, String specialCondition) {
        Optional<PreferentialConditionCache> existing = cacheRepository.findWithItemsByProductId(productId);
        if (existing.isPresent()) {
            return toConditions(existing.get());
        }
        if (!StringUtils.hasText(specialCondition)) {
            return List.of();
        }
        List<ParsedItem> parsed = parse(specialCondition);
        String sourceHash = hash(specialCondition);
        LocalDateTime now = LocalDateTime.now();
        PreferentialConditionCache cache = PreferentialConditionCache.create(productId, sourceHash, now);
        cache.replaceItems(parsed, sourceHash, now);
        cacheRepository.save(cache);
        return toParsedConditions(parsed);
    }

    /**
     * 4시 수집 배치용: 원문 변경 여부를 해시로 판단해 필요한 것만 재파싱한다.
     * 잠긴(관리자 수정) 캐시는 덮지 않고 stale 표시만 남긴다.
     */
    @Transactional
    public void refreshFromBatch(Long productId, String specialCondition) {
        String newHash = StringUtils.hasText(specialCondition) ? hash(specialCondition) : "";
        Optional<PreferentialConditionCache> existing = cacheRepository.findWithItemsByProductId(productId);

        if (existing.isEmpty()) {
            if (!StringUtils.hasText(specialCondition)) {
                return;
            }
            List<ParsedItem> parsed = parse(specialCondition);
            PreferentialConditionCache cache = PreferentialConditionCache.create(
                    productId, newHash, LocalDateTime.now());
            cache.replaceItems(parsed, newHash, LocalDateTime.now());
            cacheRepository.save(cache);
            return;
        }

        PreferentialConditionCache cache = existing.get();
        if (cache.getSourceHash().equals(newHash)) {
            return; // 원문 그대로 → 재파싱 불필요
        }
        if (cache.isEditedByAdmin()) {
            cache.markStale(newHash, LocalDateTime.now()); // 사람 수정본은 보존, 재검토 표시만
            return;
        }
        List<ParsedItem> parsed = StringUtils.hasText(specialCondition) ? parse(specialCondition) : List.of();
        cache.replaceItems(parsed, newHash, LocalDateTime.now());
    }

    /** 관리자 수동 수정: 항목 교체 + 잠금 + stale 해제. 캐시가 없으면 예외 대신 새로 만들어 잠근다. */
    @Transactional
    public void updateManually(Long productId, List<ParsedItem> items) {
        PreferentialConditionCache cache = cacheRepository.findWithItemsByProductId(productId)
                .orElseGet(() -> cacheRepository.save(
                        PreferentialConditionCache.create(productId, "", LocalDateTime.now())));
        cache.applyAdminEdit(items, LocalDateTime.now());
    }

    /** 관리자 화면: 원문이 바뀌어 재검토가 필요한(잠긴+stale) 캐시 목록. */
    @Transactional(readOnly = true)
    public List<PreferentialConditionCache> findStaleForReview() {
        return cacheRepository.findByStaleTrue();
    }

    @Transactional(readOnly = true)
    public Optional<PreferentialConditionCache> find(Long productId) {
        return cacheRepository.findWithItemsByProductId(productId);
    }

    /** LLM 우선, 실패·미가용 시 정규식 폴백, 둘 다 없으면 빈 목록. */
    private List<ParsedItem> parse(String specialCondition) {
        List<PreferentialConditionParser.ParsedCondition> parsed = llmParser.parse(specialCondition);
        if (parsed == null) {
            parsed = regexParser.parse(specialCondition);
        }
        return parsed.stream()
                .map(c -> new ParsedItem(c.description(), c.ratePercent()))
                .toList();
    }

    private List<PreferentialConditionParser.ParsedCondition> toConditions(PreferentialConditionCache cache) {
        return cache.getItems().stream()
                .map(this::toCondition)
                .toList();
    }

    private List<PreferentialConditionParser.ParsedCondition> toParsedConditions(List<ParsedItem> items) {
        return items.stream()
                .map(i -> new PreferentialConditionParser.ParsedCondition(i.description(), i.rateBonus()))
                .toList();
    }

    private PreferentialConditionParser.ParsedCondition toCondition(PreferentialConditionCacheItem item) {
        BigDecimal bonus = item.getRateBonus() == null ? BigDecimal.ZERO : item.getRateBonus();
        return new PreferentialConditionParser.ParsedCondition(item.getDescription(), bonus);
    }

    private String hash(String text) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest(text.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(bytes.length * 2);
            for (byte b : bytes) {
                sb.append(Character.forDigit((b >> 4) & 0xF, 16));
                sb.append(Character.forDigit(b & 0xF, 16));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 미지원", e); // 표준 JDK엔 항상 있음
        }
    }
}
