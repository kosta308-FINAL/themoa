package com.weaone.themoa.domain.recommend.finlife;

import java.util.ArrayList;
import java.util.List;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import com.weaone.themoa.domain.recommend.dto.AnnuityItem;
import com.weaone.themoa.domain.recommend.dto.AnnuityOptionItem;
import com.weaone.themoa.domain.recommend.dto.CreditLoanItem;
import com.weaone.themoa.domain.recommend.dto.CreditLoanOptionItem;
import com.weaone.themoa.domain.recommend.dto.FinlifeApiResponse;
import com.weaone.themoa.domain.recommend.dto.LoanBaseItem;
import com.weaone.themoa.domain.recommend.dto.LoanRateOptionItem;
import com.weaone.themoa.domain.recommend.dto.SavingsBaseItem;
import com.weaone.themoa.domain.recommend.dto.SavingsOptionItem;

/**
 * finlife API를 실제로 호출하는 클라이언트.
 * 상품 6종(예금/적금/연금/주담대/전세/신용) 각각에 대해 "권역 → 전체 페이지 순회 → 목록 합치기"를 한 번에 해준다.
 * 각 메서드는 max_page_no까지 자동으로 돌며 baseList/optionList를 전부 모아 CollectResult로 돌려준다.
 */
@Component
public class FinlifeClient {

    private final RestClient restClient;
    private final FinlifeProperties props;

    public FinlifeClient(RestClient finlifeRestClient, FinlifeProperties props) {
        this.restClient = finlifeRestClient;
        this.props = props;
    }

    /** 여러 페이지의 baseList/optionList를 하나로 합친 수집 결과. */
    public record CollectResult<B, O>(List<B> baseList, List<O> optionList) {
    }

    // ===================== 상품별 공개 메서드 =====================

    /** 예금(정기예금). topFinGrpNo: 020000 은행 / 030300 저축은행 */
    public CollectResult<SavingsBaseItem, SavingsOptionItem> fetchDeposits(String topFinGrpNo) {
        return fetchAllPages("depositProductsSearch.json", topFinGrpNo,
                new ParameterizedTypeReference<FinlifeApiResponse<SavingsBaseItem, SavingsOptionItem>>() {});
    }

    /** 적금. topFinGrpNo: 020000 은행 / 030300 저축은행 */
    public CollectResult<SavingsBaseItem, SavingsOptionItem> fetchSavings(String topFinGrpNo) {
        return fetchAllPages("savingProductsSearch.json", topFinGrpNo,
                new ParameterizedTypeReference<FinlifeApiResponse<SavingsBaseItem, SavingsOptionItem>>() {});
    }

    /** 연금저축. topFinGrpNo: 060000 금융투자. ⚠️ finlife가 현재 빈 목록을 반환하는 이슈 있음. */
    public CollectResult<AnnuityItem, AnnuityOptionItem> fetchAnnuities(String topFinGrpNo) {
        return fetchAllPages("annuitySavingProductsSearch.json", topFinGrpNo,
                new ParameterizedTypeReference<FinlifeApiResponse<AnnuityItem, AnnuityOptionItem>>() {});
    }

    /** 주택담보대출. topFinGrpNo: 020000 은행 등 */
    public CollectResult<LoanBaseItem, LoanRateOptionItem> fetchMortgageLoans(String topFinGrpNo) {
        return fetchAllPages("mortgageLoanProductsSearch.json", topFinGrpNo,
                new ParameterizedTypeReference<FinlifeApiResponse<LoanBaseItem, LoanRateOptionItem>>() {});
    }

    /** 전세자금대출. */
    public CollectResult<LoanBaseItem, LoanRateOptionItem> fetchRentLoans(String topFinGrpNo) {
        return fetchAllPages("rentHouseLoanProductsSearch.json", topFinGrpNo,
                new ParameterizedTypeReference<FinlifeApiResponse<LoanBaseItem, LoanRateOptionItem>>() {});
    }

    /** 개인신용대출. */
    public CollectResult<CreditLoanItem, CreditLoanOptionItem> fetchCreditLoans(String topFinGrpNo) {
        return fetchAllPages("creditLoanProductsSearch.json", topFinGrpNo,
                new ParameterizedTypeReference<FinlifeApiResponse<CreditLoanItem, CreditLoanOptionItem>>() {});
    }

    // ===================== 내부 공통 로직 =====================

    /** endpoint를 1페이지부터 max_page_no까지 순회하며 base/option 목록을 전부 모은다. */
    private <B, O> CollectResult<B, O> fetchAllPages(
            String endpoint, String topFinGrpNo,
            ParameterizedTypeReference<FinlifeApiResponse<B, O>> typeRef) {

        List<B> bases = new ArrayList<>();
        List<O> options = new ArrayList<>();
        int page = 1;
        int maxPage = 1;

        do {
            FinlifeApiResponse.Result<B, O> result = fetchPage(endpoint, topFinGrpNo, page, typeRef);
            if (result == null || !result.isSuccess()) {
                throw new IllegalStateException("finlife 호출 실패: " + endpoint
                        + " grp=" + topFinGrpNo + " page=" + page
                        + " err=" + (result == null ? "null" : result.errCd() + "/" + result.errMsg()));
            }
            if (result.baseList() != null) {
                bases.addAll(result.baseList());
            }
            if (result.optionList() != null) {
                options.addAll(result.optionList());
            }
            maxPage = result.maxPageNo() == null ? 1 : result.maxPageNo();
            page++;
        } while (page <= maxPage);

        return new CollectResult<>(bases, options);
    }

    /** endpoint 한 페이지 호출 후 result 부분만 반환. */
    private <B, O> FinlifeApiResponse.Result<B, O> fetchPage(
            String endpoint, String topFinGrpNo, int pageNo,
            ParameterizedTypeReference<FinlifeApiResponse<B, O>> typeRef) {

        FinlifeApiResponse<B, O> response = restClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/" + endpoint)
                        .queryParam("auth", props.apiKey())
                        .queryParam("topFinGrpNo", topFinGrpNo)
                        .queryParam("pageNo", pageNo)
                        .build())
                .retrieve()
                .body(typeRef);

        return response == null ? null : response.result();
    }
}
