package com.weaone.themoa.domain.financialsearch.service;

import com.weaone.themoa.domain.financialsearch.dto.request.BankLinkSaveRequest;
import com.weaone.themoa.domain.financialsearch.dto.response.BankLinkListResponse;
import com.weaone.themoa.domain.financialsearch.entity.BankLink;
import com.weaone.themoa.domain.financialsearch.repository.BankLinkRepository;
import com.weaone.themoa.domain.financialsearch.repository.FinancialLoanSearchRepository;
import com.weaone.themoa.domain.financialsearch.repository.FinancialSavingsSearchRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.TreeSet;

/**
 * 은행 공식 링크 관리(관리자 전용).
 *
 * <p>링크가 없는 회사의 상품은 결과 화면에서 공식 홈페이지 대신 검색 링크로 나가므로, 어떤 회사에
 * 링크가 비어 있는지 함께 알려준다.
 *
 * <p>변경 후에는 반드시 {@link BankUrlResolver#refresh()}를 불러 캐시를 갱신한다. 그러지 않으면
 * 등록해도 서버를 재시작하기 전까지 화면에 반영되지 않는다.
 */
@Service
public class BankLinkAdminService {

    private final BankLinkRepository bankLinkRepository;
    private final FinancialSavingsSearchRepository savingsProductRepository;
    private final FinancialLoanSearchRepository loanProductRepository;
    private final BankUrlResolver bankUrlResolver;

    public BankLinkAdminService(BankLinkRepository bankLinkRepository,
                                FinancialSavingsSearchRepository savingsProductRepository,
                                FinancialLoanSearchRepository loanProductRepository,
                                BankUrlResolver bankUrlResolver) {
        this.bankLinkRepository = bankLinkRepository;
        this.savingsProductRepository = savingsProductRepository;
        this.loanProductRepository = loanProductRepository;
        this.bankUrlResolver = bankUrlResolver;
    }

    @Transactional(readOnly = true)
    public BankLinkListResponse findAll() {
        List<BankLinkListResponse.BankLinkResponse> links = bankLinkRepository.findAll().stream()
                .map(link -> new BankLinkListResponse.BankLinkResponse(link.getCompanyName(), link.getOfficialUrl()))
                .sorted(Comparator.comparing(BankLinkListResponse.BankLinkResponse::companyName))
                .toList();

        // 상품에 등장하는 회사명을 모아, 검증된 링크로 연결되지 않는 것만 남긴다.
        // 부분 매칭("농협은행" ↔ "농협은행주식회사")도 링크가 있는 것으로 보기 위해 resolver의 판단을 그대로 쓴다.
        TreeSet<String> companies = new TreeSet<>();
        companies.addAll(savingsProductRepository.findDistinctCompanyNames());
        companies.addAll(loanProductRepository.findDistinctCompanyNames());
        List<String> companiesWithoutLink = companies.stream()
                .filter(company -> !bankUrlResolver.hasVerifiedUrl(company))
                .toList();

        return new BankLinkListResponse(links, companiesWithoutLink);
    }

    /**
     * 등록 또는 수정. 회사명이 식별자라 이미 있으면 URL만 갱신한다.
     *
     * @return 이번 호출로 새로 등록했으면 true
     */
    @Transactional
    public boolean save(BankLinkSaveRequest request) {
        String companyName = request.companyName().trim();
        String officialUrl = request.officialUrl().trim();

        Optional<BankLink> existing = bankLinkRepository.findById(companyName);
        boolean created = existing.isEmpty();
        if (created) {
            bankLinkRepository.save(BankLink.of(companyName, officialUrl));
        } else {
            existing.get().changeOfficialUrl(officialUrl);
        }
        bankUrlResolver.refresh();
        return created;
    }

    /** 삭제. 없는 회사명이어도 오류 없이 넘어간다(이미 없는 상태와 결과가 같다). */
    @Transactional
    public void delete(String companyName) {
        bankLinkRepository.deleteById(companyName);
        bankUrlResolver.refresh();
    }
}
