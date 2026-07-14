package com.weaone.themoa.domain.category.repository;

import com.weaone.themoa.domain.category.entity.Category;
import com.weaone.themoa.domain.category.entity.CategoryCode;
import com.weaone.themoa.domain.category.entity.CategoryKeywordRule;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * priority·길이 정렬이 실제 MySQL LIKE + ORDER BY에서도 문서(category.md §5) 순서대로 동작하는지 검증.
 * 시드는 {@code CategorySeeder}가 애플리케이션 기동 시 이미 채워 둔다(테스트 DB는 실제 MySQL, H2 미사용).
 */
@SpringBootTest
@Transactional
class CategoryKeywordRuleRepositoryTest {

    @Autowired
    private CategoryKeywordRuleRepository categoryKeywordRuleRepository;
    @Autowired
    private CategoryRepository categoryRepository;

    @Test
    @DisplayName("길이가 겹치는 쿠팡 계열은 더 구체적인(긴) 키워드가 먼저 매칭된다")
    void coupangFamilyOrderedByKeywordLengthDescending() {
        List<CategoryKeywordRule> result = categoryKeywordRuleRepository.findMatchingOrderByPriority("쿠팡(쿠페이)");

        assertThat(result).isNotEmpty();
        assertThat(result.get(0).getKeyword()).isEqualTo("쿠팡(쿠페이)");
        assertThat(result.get(0).getCategory().getCode()).isEqualTo(CategoryCode.SUBSCRIPTION.name());
    }

    @Test
    @DisplayName("쿠팡이츠는 쿠팡보다 먼저 매칭된다")
    void coupangEatsMatchesBeforeCoupang() {
        List<CategoryKeywordRule> result = categoryKeywordRuleRepository.findMatchingOrderByPriority("쿠팡이츠 강남점");

        assertThat(result.get(0).getKeyword()).isEqualTo("쿠팡이츠");
        assertThat(result.get(0).getCategory().getCode()).isEqualTo(CategoryCode.DELIVERY.name());
    }

    @Test
    @DisplayName("순수 쇼핑 건은 쿠팡만 매칭된다")
    void plainCoupangMatchesShoppingOnly() {
        List<CategoryKeywordRule> result = categoryKeywordRuleRepository.findMatchingOrderByPriority("쿠팡 주식회사");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getKeyword()).isEqualTo("쿠팡");
        assertThat(result.get(0).getCategory().getCode()).isEqualTo(CategoryCode.SHOPPING.name());
    }

    @Test
    @DisplayName("아무 키워드도 포함하지 않으면 매칭 결과가 없다")
    void noKeywordMatchesReturnsEmpty() {
        List<CategoryKeywordRule> result = categoryKeywordRuleRepository.findMatchingOrderByPriority("복뚱이네 간장게장");

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("카테고리 마스터 12종이 시드돼 있다")
    void categoriesAreSeeded() {
        List<Category> categories = categoryRepository.findAll();

        assertThat(categories).hasSize(12);
        assertThat(categories).extracting(Category::getCode)
                .containsExactlyInAnyOrder(
                        "FOOD", "DELIVERY", "CAFE", "CONVENIENCE", "TRANSPORT", "SHOPPING",
                        "SUBSCRIPTION", "LEISURE", "MEDICAL", "BEAUTY", "DONATION", "ETC");
    }
}
