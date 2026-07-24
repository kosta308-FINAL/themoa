package com.weaone.themoa.domain.bookmark.service;

import com.weaone.themoa.domain.bookmark.entity.BookmarkTargetType;
import com.weaone.themoa.domain.bookmark.repository.BookmarkLoanProductRepository;
import com.weaone.themoa.domain.financialsearch.service.BankNameFormatter;
import com.weaone.themoa.domain.financialsearch.service.BankUrlResolver;
import com.weaone.themoa.domain.recommend.entity.LoanProduct;
import com.weaone.themoa.domain.recommend.entity.LoanProductOption;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * 대출 북마크 상세. 대표금리는 예·적금과 반대로 "최저금리"를 쓴다(대출은 낮을수록 유리).
 * 대출에는 가입기간 개념이 없어 termMonth는 null이다.
 */
@Component
public class LoanProductBookmarkTargetReader implements BookmarkTargetReader {

    private final BookmarkLoanProductRepository loanProductRepository;
    private final BankUrlResolver bankUrlResolver;
    private final BankNameFormatter bankNameFormatter;

    public LoanProductBookmarkTargetReader(BookmarkLoanProductRepository loanProductRepository,
                                           BankUrlResolver bankUrlResolver,
                                              BankNameFormatter bankNameFormatter) {
        this.loanProductRepository = loanProductRepository;
        this.bankUrlResolver = bankUrlResolver;
        this.bankNameFormatter = bankNameFormatter;
    }

    @Override
    public BookmarkTargetType supportedType() {
        return BookmarkTargetType.LOAN_PRODUCT;
    }

    @Override
    public Map<Long, BookmarkTargetDetail> readAll(Collection<Long> targetIds) {
        if (targetIds.isEmpty()) {
            return Map.of();
        }
        Map<Long, BookmarkTargetDetail> details = new LinkedHashMap<>();
        for (LoanProduct product : loanProductRepository.findAllWithOptionsByIdIn(targetIds)) {
            BigDecimal minRate = product.getOptions().stream()
                    .map(LoanProductOption::getRateMin)
                    .filter(Objects::nonNull)
                    .min(Comparator.naturalOrder())
                    .orElse(null);
            details.put(product.getId(), new BookmarkTargetDetail(
                    product.getProductName(),
                    bankNameFormatter.toDisplayName(product.getCompanyName()),
                    product.getProductType() == null ? null : product.getProductType().name(),
                    product.getJoinMethod(),
                    minRate,
                    null,
                    product.getSpecialCondition(),
                    bankUrlResolver.resolve(product.getCompanyName()),
                    product.getCloseDate() != null));
        }
        return details;
    }
}
