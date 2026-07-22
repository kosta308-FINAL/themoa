package com.weaone.themoa.domain.financialsearch.service;

import com.weaone.themoa.domain.financialsearch.config.FinancialRagProperties;
import com.weaone.themoa.domain.financialsearch.dto.response.BankLinkListResponse;
import com.weaone.themoa.domain.financialsearch.dto.response.FinancialStatusResponse;
import com.weaone.themoa.domain.financialsearch.repository.FinancialLoanSearchRepository;
import com.weaone.themoa.domain.financialsearch.repository.FinancialSavingsSearchRepository;
import io.qdrant.client.QdrantClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.Objects;
import java.util.stream.Stream;

/**
 * 관리자 대시보드용 금융상품 데이터 현황 조회.
 *
 * <p>상품 수와 벡터 인덱스 문서 수를 나란히 보여주는 게 핵심이다. 두 숫자가 다르면 수집 이후 인덱스
 * 갱신을 하지 않았다는 뜻이므로, 관리자가 "검색 인덱스 갱신"을 눌러야 한다는 걸 바로 알 수 있다.
 */
@Service
public class FinancialStatusService {

    private static final Logger log = LoggerFactory.getLogger(FinancialStatusService.class);
    /** 현황 조회 때문에 화면이 오래 멈추면 안 되므로 Qdrant 조회는 짧게 끊는다. */
    private static final Duration QDRANT_TIMEOUT = Duration.ofSeconds(3);

    private final FinancialSavingsSearchRepository savingsProductRepository;
    private final FinancialLoanSearchRepository loanProductRepository;
    private final BankLinkAdminService bankLinkAdminService;
    private final FinancialRagProperties ragProperties;
    private final ObjectProvider<QdrantClient> qdrantClientProvider;

    public FinancialStatusService(FinancialSavingsSearchRepository savingsProductRepository,
                                  FinancialLoanSearchRepository loanProductRepository,
                                  BankLinkAdminService bankLinkAdminService,
                                  FinancialRagProperties ragProperties,
                                  ObjectProvider<QdrantClient> qdrantClientProvider) {
        this.savingsProductRepository = savingsProductRepository;
        this.loanProductRepository = loanProductRepository;
        this.bankLinkAdminService = bankLinkAdminService;
        this.ragProperties = ragProperties;
        this.qdrantClientProvider = qdrantClientProvider;
    }

    @Transactional(readOnly = true)
    public FinancialStatusResponse getStatus() {
        FinancialStatusResponse.ProductCount savings = new FinancialStatusResponse.ProductCount(
                savingsProductRepository.count(), savingsProductRepository.countByCloseDateIsNull());
        FinancialStatusResponse.ProductCount loans = new FinancialStatusResponse.ProductCount(
                loanProductRepository.count(), loanProductRepository.countByCloseDateIsNull());

        LocalDateTime lastCollectedAt = Stream.of(
                        savingsProductRepository.findLastUpdatedAt(),
                        loanProductRepository.findLastUpdatedAt())
                .filter(Objects::nonNull)
                .max(Comparator.naturalOrder())
                .orElse(null);

        BankLinkListResponse bankLinks = bankLinkAdminService.findAll();
        FinancialStatusResponse.BankLinkStatus bankLinkStatus = new FinancialStatusResponse.BankLinkStatus(
                bankLinks.links().size(), bankLinks.companiesWithoutLink().size());

        return new FinancialStatusResponse(savings, loans, lastCollectedAt,
                ragProperties.isEnabled(), countIndexedDocuments(), bankLinkStatus);
    }

    /** 벡터 인덱스 문서 수. 벡터검색이 꺼져 있거나 Qdrant에 닿지 않으면 null(화면에서 "-"로 표시). */
    private Long countIndexedDocuments() {
        if (!ragProperties.isEnabled()) {
            return null;
        }
        QdrantClient qdrantClient = qdrantClientProvider.getIfAvailable();
        if (qdrantClient == null) {
            return null;
        }
        try {
            return qdrantClient.countAsync(ragProperties.getCollectionName(), QDRANT_TIMEOUT).get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return null;
        } catch (Exception e) {
            // 현황 조회는 부가 정보라, Qdrant가 죽어 있어도 나머지 현황은 보여준다.
            log.warn("Qdrant 문서 수 조회 실패 — 현황에서 제외합니다. collection={}",
                    ragProperties.getCollectionName(), e);
            return null;
        }
    }
}
