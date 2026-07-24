package com.weaone.themoa.domain.financialsearch.service;

import com.weaone.themoa.domain.bookmark.entity.BookmarkTargetType;
import com.weaone.themoa.domain.bookmark.repository.BookmarkRepository;
import com.weaone.themoa.domain.bookmark.repository.BookmarkSavingsProductRepository;
import com.weaone.themoa.domain.financialsearch.dto.response.PopularProductResponse;
import com.weaone.themoa.domain.recommend.entity.SavingsProduct;
import com.weaone.themoa.domain.recommend.entity.SavingsProductOption;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 실시간 인기 금융상품(예·적금) 순위. 회원들이 북마크한 수가 많은 순으로 보여준다.
 *
 * <p>추천 화면은 "내 조건 기준" 개인화 결과인데, 이 순위는 "전체 사용자 기준"이라 성격이 달라 함께 둘 만하다.
 */
@Service
public class PopularFinancialProductService {

    private static final int DEFAULT_LIMIT = 5;
    private static final int MAX_LIMIT = 20;

    private final BookmarkRepository bookmarkRepository;
    private final BookmarkSavingsProductRepository savingsProductRepository;
    private final BankUrlResolver bankUrlResolver;
    private final BankNameFormatter bankNameFormatter;

    public PopularFinancialProductService(BookmarkRepository bookmarkRepository,
                                          BookmarkSavingsProductRepository savingsProductRepository,
                                          BankUrlResolver bankUrlResolver,
                                          BankNameFormatter bankNameFormatter) {
        this.bookmarkRepository = bookmarkRepository;
        this.savingsProductRepository = savingsProductRepository;
        this.bankUrlResolver = bankUrlResolver;
        this.bankNameFormatter = bankNameFormatter;
    }

    @Transactional(readOnly = true)
    public List<PopularProductResponse> findPopular(int limit) {
        int size = Math.max(1, Math.min(limit, MAX_LIMIT));
        List<BookmarkRepository.PopularTarget> popular = bookmarkRepository.findPopularTargets(
                BookmarkTargetType.SAVINGS_PRODUCT, PageRequest.of(0, size));
        if (popular.isEmpty()) {
            return List.of();
        }

        // 상품 상세를 한 번에 조회(순위 개수만큼 쿼리가 나가지 않도록).
        List<Long> ids = popular.stream().map(BookmarkRepository.PopularTarget::getTargetId).toList();
        Map<Long, SavingsProduct> productById = new LinkedHashMap<>();
        for (SavingsProduct product : savingsProductRepository.findAllWithOptionsByIdIn(ids)) {
            productById.put(product.getId(), product);
        }

        List<PopularProductResponse> responses = new ArrayList<>();
        int rank = 0;
        for (BookmarkRepository.PopularTarget target : popular) {
            SavingsProduct product = productById.get(target.getTargetId());
            if (product == null) {
                // 상품이 삭제됐으면 순위에서 제외한다(순번은 남은 것으로 다시 매긴다).
                continue;
            }
            rank++;
            BigDecimal bestRate = product.getOptions().stream()
                    .map(SavingsProductOption::getMaxRate)
                    .filter(Objects::nonNull)
                    .max(Comparator.naturalOrder())
                    .orElse(null);
            Integer shortestTerm = product.getOptions().stream()
                    .map(SavingsProductOption::getTermMonth)
                    .filter(Objects::nonNull)
                    .min(Comparator.naturalOrder())
                    .orElse(null);
            responses.add(new PopularProductResponse(
                    rank,
                    product.getId(),
                    product.getProductName(),
                    bankNameFormatter.toDisplayName(product.getCompanyName()),
                    product.getProductType() == null ? null : product.getProductType().name(),
                    bestRate,
                    shortestTerm,
                    target.getCount(),
                    bankUrlResolver.resolve(product.getCompanyName())));
        }
        return responses;
    }

    public int defaultLimit() {
        return DEFAULT_LIMIT;
    }
}
