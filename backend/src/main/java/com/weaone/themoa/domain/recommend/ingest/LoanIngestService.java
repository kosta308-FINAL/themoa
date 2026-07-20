package com.weaone.themoa.domain.recommend.ingest;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.weaone.themoa.domain.recommend.entity.LoanProduct;
import com.weaone.themoa.domain.recommend.entity.LoanProductOption;
import com.weaone.themoa.domain.recommend.repository.LoanProductRepository;
import com.weaone.themoa.domain.recommend.entity.LoanType;
import com.weaone.themoa.domain.recommend.finlife.FinlifeClient;
import com.weaone.themoa.domain.recommend.finlife.FinlifeClient.CollectResult;
import com.weaone.themoa.domain.recommend.dto.CreditLoanItem;
import com.weaone.themoa.domain.recommend.dto.CreditLoanOptionItem;
import com.weaone.themoa.domain.recommend.dto.LoanBaseItem;
import com.weaone.themoa.domain.recommend.dto.LoanRateOptionItem;

/**
 * 대출 3종(주담대/전세/신용) 수집→저장 서비스 (배치 4단계 중 대출 파트).
 * 예적금과 흐름은 같으나, 신용대출은 옵션을 신용점수 구간별로 펼쳐 저장한다.
 */
@Service
public class LoanIngestService {

    /** 대출 수집 권역: 020000 은행. (필요 시 저축은행 등 추가 가능) */
    private static final String GROUP = "020000";

    private final FinlifeClient finlifeClient;
    private final LoanProductRepository repository;

    public LoanIngestService(FinlifeClient finlifeClient, LoanProductRepository repository) {
        this.finlifeClient = finlifeClient;
        this.repository = repository;
    }

    @Transactional
    public IngestSummary ingestAll() {
        IngestSummary summary = new IngestSummary();
        ingestMortgageRent(finlifeClient.fetchMortgageLoans(GROUP), LoanType.MORTGAGE, summary);
        ingestMortgageRent(finlifeClient.fetchRentLoans(GROUP), LoanType.RENT, summary);
        ingestCredit(finlifeClient.fetchCreditLoans(GROUP), summary);
        return summary;
    }

    /** 주담대/전세: 옵션을 (회사+상품코드)로 묶어 매칭 후 Upsert. */
    private void ingestMortgageRent(CollectResult<LoanBaseItem, LoanRateOptionItem> result,
                                    LoanType type, IngestSummary summary) {
        Map<String, List<LoanRateOptionItem>> optionsByKey = result.optionList().stream()
                .collect(Collectors.groupingBy(o -> key(o.finCoNo(), o.finPrdtCd())));

        for (LoanBaseItem base : result.baseList()) {
            summary.fetched++;
            if (isClosed(base.dclsEndDay())) {
                summary.skippedClosed++;
                continue;
            }
            List<LoanProductOption> options = optionsByKey
                    .getOrDefault(key(base.finCoNo(), base.finPrdtCd()), List.of())
                    .stream()
                    .map(LoanMapper::toRateOption)
                    .toList();

            upsert(LoanMapper.toMortgageRentProduct(base, type), options, summary);
        }
    }

    /** 신용대출: 옵션 1건이 여러 구간 옵션으로 펼쳐지므로 flatMap으로 모은다. */
    private void ingestCredit(CollectResult<CreditLoanItem, CreditLoanOptionItem> result,
                              IngestSummary summary) {
        Map<String, List<CreditLoanOptionItem>> optionsByKey = result.optionList().stream()
                .collect(Collectors.groupingBy(o -> key(o.finCoNo(), o.finPrdtCd())));

        for (CreditLoanItem base : result.baseList()) {
            summary.fetched++;
            if (isClosed(base.dclsEndDay())) {
                summary.skippedClosed++;
                continue;
            }
            List<LoanProductOption> options = optionsByKey
                    .getOrDefault(key(base.finCoNo(), base.finPrdtCd()), List.of())
                    .stream()
                    .flatMap(o -> LoanMapper.toCreditOptions(o).stream())
                    .toList();

            upsert(LoanMapper.toCreditProduct(base), options, summary);
        }
    }

    /** 공통 Upsert: incoming(신규 매핑 결과)을 기존이 있으면 그 필드로 갱신, 없으면 새로 저장. */
    private void upsert(LoanProduct incoming, List<LoanProductOption> options, IngestSummary summary) {
        repository.findByCompanyCodeAndProductCodeAndProductType(
                        incoming.getCompanyCode(), incoming.getProductCode(), incoming.getProductType())
                .ifPresentOrElse(existing -> {
                    existing.updateBasicInfo(incoming.getCompanyName(), incoming.getProductName(),
                            incoming.getJoinMethod(), incoming.getLoanLimit(), incoming.getEarlyRepayFee(),
                            incoming.getSpecialCondition(), incoming.getMaturityInterest(),
                            incoming.getDelayRate(), incoming.getNote(), incoming.getMaxAmount(),
                            incoming.getOpenDate(), incoming.getCloseDate());
                    existing.replaceOptions(options);
                    summary.updated++;
                }, () -> {
                    options.forEach(incoming::addOption);
                    repository.save(incoming);
                    summary.inserted++;
                });
    }

    private static boolean isClosed(String dclsEndDay) {
        return dclsEndDay != null && !dclsEndDay.isBlank();
    }

    private static String key(String companyCode, String productCode) {
        return companyCode + "|" + productCode;
    }

    /** 수집 집계 결과. */
    public static class IngestSummary {
        public int fetched;
        public int skippedClosed;
        public int inserted;
        public int updated;

        public int saved() {
            return inserted + updated;
        }

        @Override
        public String toString() {
            return "받음 " + fetched + " / 판매종료제외 " + skippedClosed
                    + " / 신규 " + inserted + " / 갱신 " + updated + " (저장합계 " + saved() + ")";
        }
    }
}
