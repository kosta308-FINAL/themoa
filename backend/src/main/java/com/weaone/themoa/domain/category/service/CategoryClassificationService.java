package com.weaone.themoa.domain.category.service;

import com.weaone.themoa.domain.category.entity.Category;
import com.weaone.themoa.domain.category.entity.CategoryCode;
import com.weaone.themoa.domain.category.entity.CategoryKeywordRule;
import com.weaone.themoa.domain.category.entity.MerchantTypeCategoryMap;
import com.weaone.themoa.domain.category.repository.CategoryKeywordRuleRepository;
import com.weaone.themoa.domain.category.repository.CategoryRepository;
import com.weaone.themoa.domain.category.repository.MerchantTypeCategoryMapRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Optional;

/**
 * 카테고리 자동 분류 파이프라인의 단일 진입점(category.md §2): ①가맹점명 키워드(부분일치, priority→길이
 * 내림차순) → ②업종 매핑(완전일치) → ③둘 다 실패하면 '기타'. 사용자 건별 수정 보호(§2-④)와 거래 저장
 * 시점 스냅샷 반영은 card_transaction 도메인이 구현되면 그 저장 로직이 이 서비스의 반환값을 사용해 채운다
 * — 이 서비스 자체는 분류 결과만 계산해서 돌려준다.
 */
@Service
@RequiredArgsConstructor
public class CategoryClassificationService {

    private final CategoryRepository categoryRepository;
    private final CategoryKeywordRuleRepository categoryKeywordRuleRepository;
    private final MerchantTypeCategoryMapRepository merchantTypeCategoryMapRepository;

    @Transactional(readOnly = true)
    public Category classify(String merchantNameRaw, String merchantTypeRaw) {
        return matchByKeyword(merchantNameRaw)
                .or(() -> matchByMerchantType(merchantTypeRaw))
                .orElseGet(this::etcCategory);
    }

    private Optional<Category> matchByKeyword(String merchantNameRaw) {
        if (!StringUtils.hasText(merchantNameRaw)) {
            return Optional.empty();
        }
        return categoryKeywordRuleRepository.findMatchingOrderByPriority(merchantNameRaw).stream()
                .findFirst()
                .map(CategoryKeywordRule::getCategory);
    }

    private Optional<Category> matchByMerchantType(String merchantTypeRaw) {
        if (!StringUtils.hasText(merchantTypeRaw)) {
            return Optional.empty();
        }
        return merchantTypeCategoryMapRepository.findByMerchantType(merchantTypeRaw)
                .map(MerchantTypeCategoryMap::getCategory);
    }

    private Category etcCategory() {
        return categoryRepository.findByCode(CategoryCode.ETC.name())
                .orElseThrow(() -> new IllegalStateException("ETC 카테고리가 시드되지 않았습니다."));
    }
}
