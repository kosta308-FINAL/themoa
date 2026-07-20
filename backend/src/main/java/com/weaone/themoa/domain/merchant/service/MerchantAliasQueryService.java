package com.weaone.themoa.domain.merchant.service;

import com.weaone.themoa.domain.merchant.entity.MerchantAlias;
import com.weaone.themoa.domain.merchant.repository.MerchantAliasRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

/** F-03 가맹점 검색/선택 드롭다운(view/fixedExpense.md §3.3) 조회 전용. alias는 회원 소유가 아니라 전역 카탈로그다(merchant.md §1). */
@Service
@RequiredArgsConstructor
public class MerchantAliasQueryService {

    private static final int SEARCH_LIMIT = 20;

    private final MerchantAliasRepository merchantAliasRepository;

    @Transactional(readOnly = true)
    public List<MerchantAlias> search(String query) {
        Pageable top20 = PageRequest.of(0, SEARCH_LIMIT);
        if (!StringUtils.hasText(query)) {
            return merchantAliasRepository.findByOrderByCanonicalServiceNameAsc(top20);
        }
        return merchantAliasRepository
                .findByCanonicalServiceNameContainingIgnoreCaseOrderByCanonicalServiceNameAsc(query.trim(), top20);
    }
}
