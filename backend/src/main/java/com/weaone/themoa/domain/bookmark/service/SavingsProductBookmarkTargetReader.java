package com.weaone.themoa.domain.bookmark.service;

import com.weaone.themoa.domain.bookmark.entity.BookmarkTargetType;
import com.weaone.themoa.domain.bookmark.repository.BookmarkSavingsProductRepository;
import com.weaone.themoa.domain.recommend.entity.SavingsProduct;
import com.weaone.themoa.domain.recommend.entity.SavingsProductOption;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/** 예·적금 북마크 상세. 대표금리는 옵션 중 최고금리, 대표기간은 가장 짧은 가입기간으로 보여준다. */
@Component
public class SavingsProductBookmarkTargetReader implements BookmarkTargetReader {

    private final BookmarkSavingsProductRepository savingsProductRepository;

    public SavingsProductBookmarkTargetReader(BookmarkSavingsProductRepository savingsProductRepository) {
        this.savingsProductRepository = savingsProductRepository;
    }

    @Override
    public BookmarkTargetType supportedType() {
        return BookmarkTargetType.SAVINGS_PRODUCT;
    }

    @Override
    public Map<Long, BookmarkTargetDetail> readAll(Collection<Long> targetIds) {
        if (targetIds.isEmpty()) {
            // 빈 컬렉션으로 in 절을 만들면 DB에 따라 오류가 나므로 조회 자체를 건너뛴다.
            return Map.of();
        }
        Map<Long, BookmarkTargetDetail> details = new LinkedHashMap<>();
        for (SavingsProduct product : savingsProductRepository.findAllWithOptionsByIdIn(targetIds)) {
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
            details.put(product.getId(), new BookmarkTargetDetail(
                    product.getProductName(), product.getCompanyName(), bestRate, shortestTerm));
        }
        return details;
    }
}
