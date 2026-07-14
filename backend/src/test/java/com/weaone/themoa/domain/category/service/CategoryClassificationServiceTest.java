package com.weaone.themoa.domain.category.service;

import com.weaone.themoa.domain.category.entity.Category;
import com.weaone.themoa.domain.category.entity.CategoryCode;
import com.weaone.themoa.domain.category.entity.CategoryKeywordRule;
import com.weaone.themoa.domain.category.entity.MerchantTypeCategoryMap;
import com.weaone.themoa.domain.category.repository.CategoryKeywordRuleRepository;
import com.weaone.themoa.domain.category.repository.CategoryRepository;
import com.weaone.themoa.domain.category.repository.MerchantTypeCategoryMapRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
class CategoryClassificationServiceTest {

    @Mock
    private CategoryRepository categoryRepository;
    @Mock
    private CategoryKeywordRuleRepository categoryKeywordRuleRepository;
    @Mock
    private MerchantTypeCategoryMapRepository merchantTypeCategoryMapRepository;

    @InjectMocks
    private CategoryClassificationService categoryClassificationService;

    private Category categoryWithId(long id, CategoryCode code) {
        Category category = Category.seed(code, code.name());
        ReflectionTestUtils.setField(category, "id", id);
        return category;
    }

    @Test
    @DisplayName("가맹점명 키워드가 매칭되면 업종 매핑은 조회하지 않는다(①이 ②보다 우선)")
    void classifyPrefersKeywordOverMerchantType() {
        Category delivery = categoryWithId(2L, CategoryCode.DELIVERY);
        CategoryKeywordRule rule = CategoryKeywordRule.seed("쿠팡이츠", delivery);
        given(categoryKeywordRuleRepository.findMatchingOrderByPriority("쿠팡이츠 강남점"))
                .willReturn(List.of(rule));

        Category result = categoryClassificationService.classify("쿠팡이츠 강남점", "전자상거래(다품목취급)");

        assertThat(result).isEqualTo(delivery);
        then(merchantTypeCategoryMapRepository).should(never()).findByMerchantType(any());
    }

    @Test
    @DisplayName("키워드가 매칭되지 않으면 업종 매핑으로 분류한다")
    void classifyFallsBackToMerchantType() {
        Category convenience = categoryWithId(4L, CategoryCode.CONVENIENCE);
        given(categoryKeywordRuleRepository.findMatchingOrderByPriority("지에스25 역삼점")).willReturn(List.of());
        given(merchantTypeCategoryMapRepository.findByMerchantType("편의점"))
                .willReturn(Optional.of(MerchantTypeCategoryMap.seed("편의점", convenience)));

        Category result = categoryClassificationService.classify("지에스25 역삼점", "편의점");

        assertThat(result).isEqualTo(convenience);
    }

    @Test
    @DisplayName("키워드·업종 매핑 둘 다 실패하면 '기타'로 분류한다")
    void classifyFallsBackToEtc() {
        Category etc = categoryWithId(12L, CategoryCode.ETC);
        given(categoryKeywordRuleRepository.findMatchingOrderByPriority("비바리퍼블리카")).willReturn(List.of());
        given(merchantTypeCategoryMapRepository.findByMerchantType("결제대행(PG)")).willReturn(Optional.empty());
        given(categoryRepository.findByCode(CategoryCode.ETC.name())).willReturn(Optional.of(etc));

        Category result = categoryClassificationService.classify("비바리퍼블리카", "결제대행(PG)");

        assertThat(result).isEqualTo(etc);
    }

    @Test
    @DisplayName("업종명이 빈값이어도 업종 매핑을 조회하지 않고 바로 '기타'로 떨어진다")
    void classifyHandlesBlankMerchantType() {
        Category etc = categoryWithId(12L, CategoryCode.ETC);
        given(categoryKeywordRuleRepository.findMatchingOrderByPriority("해외가맹점")).willReturn(List.of());
        given(categoryRepository.findByCode(CategoryCode.ETC.name())).willReturn(Optional.of(etc));

        Category result = categoryClassificationService.classify("해외가맹점", "");

        assertThat(result).isEqualTo(etc);
        then(merchantTypeCategoryMapRepository).should(never()).findByMerchantType(any());
    }
}
