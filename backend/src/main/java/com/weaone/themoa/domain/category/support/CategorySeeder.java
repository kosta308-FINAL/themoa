package com.weaone.themoa.domain.category.support;

import com.weaone.themoa.domain.category.entity.Category;
import com.weaone.themoa.domain.category.entity.CategoryCode;
import com.weaone.themoa.domain.category.entity.CategoryKeywordRule;
import com.weaone.themoa.domain.category.entity.MerchantTypeCategoryMap;
import com.weaone.themoa.domain.category.repository.CategoryKeywordRuleRepository;
import com.weaone.themoa.domain.category.repository.CategoryRepository;
import com.weaone.themoa.domain.category.repository.MerchantTypeCategoryMapRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 카테고리 전역 마스터 시드(category.md §3~§5). {@link com.weaone.themoa.domain.merchant.support.MerchantAliasSeeder}가
 * 카테고리 FK를 참조하므로 반드시 그보다 먼저 실행돼야 한다({@link Order}).
 */
@Component
@Order(1)
@RequiredArgsConstructor
public class CategorySeeder implements ApplicationRunner {

    private final CategoryRepository categoryRepository;
    private final MerchantTypeCategoryMapRepository merchantTypeCategoryMapRepository;
    private final CategoryKeywordRuleRepository categoryKeywordRuleRepository;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        Map<CategoryCode, Category> categories = seedCategories();
        seedMerchantTypeMap(categories);
        seedKeywordRules(categories);
    }

    private Map<CategoryCode, Category> seedCategories() {
        if (categoryRepository.count() > 0) {
            return categoryRepository.findAll().stream()
                    .collect(Collectors.toMap(c -> CategoryCode.valueOf(c.getCode()), c -> c));
        }
        List<Category> saved = categoryRepository.saveAll(List.of(
                Category.seed(CategoryCode.FOOD, "식비"),
                Category.seed(CategoryCode.DELIVERY, "배달"),
                Category.seed(CategoryCode.CAFE, "카페"),
                Category.seed(CategoryCode.CONVENIENCE, "편의점/마트"),
                Category.seed(CategoryCode.TRANSPORT, "교통"),
                Category.seed(CategoryCode.SHOPPING, "쇼핑"),
                Category.seed(CategoryCode.SUBSCRIPTION, "구독"),
                Category.seed(CategoryCode.LEISURE, "여가"),
                Category.seed(CategoryCode.MEDICAL, "의료"),
                Category.seed(CategoryCode.BEAUTY, "미용"),
                Category.seed(CategoryCode.DONATION, "기부/회비"),
                Category.seed(CategoryCode.ETC, "기타")
        ));
        return saved.stream().collect(Collectors.toMap(c -> CategoryCode.valueOf(c.getCode()), c -> c));
    }

    private void seedMerchantTypeMap(Map<CategoryCode, Category> categories) {
        if (merchantTypeCategoryMapRepository.count() > 0) {
            return;
        }
        merchantTypeCategoryMapRepository.saveAll(List.of(
                MerchantTypeCategoryMap.seed("편의점", categories.get(CategoryCode.CONVENIENCE)),
                MerchantTypeCategoryMap.seed("할인점/슈퍼마켓", categories.get(CategoryCode.CONVENIENCE)),
                MerchantTypeCategoryMap.seed("식품잡화", categories.get(CategoryCode.CONVENIENCE)),
                MerchantTypeCategoryMap.seed("한식", categories.get(CategoryCode.FOOD)),
                MerchantTypeCategoryMap.seed("일반대중음식", categories.get(CategoryCode.FOOD)),
                MerchantTypeCategoryMap.seed("패스트푸드", categories.get(CategoryCode.FOOD)),
                MerchantTypeCategoryMap.seed("중식", categories.get(CategoryCode.FOOD)),
                MerchantTypeCategoryMap.seed("일식", categories.get(CategoryCode.FOOD)),
                MerchantTypeCategoryMap.seed("커피전문점", categories.get(CategoryCode.CAFE)),
                MerchantTypeCategoryMap.seed("택시", categories.get(CategoryCode.TRANSPORT)),
                MerchantTypeCategoryMap.seed("노래방", categories.get(CategoryCode.LEISURE)),
                MerchantTypeCategoryMap.seed("PC게임방", categories.get(CategoryCode.LEISURE)),
                MerchantTypeCategoryMap.seed("스포츠센타/레포츠클럽", categories.get(CategoryCode.LEISURE)),
                MerchantTypeCategoryMap.seed("약국", categories.get(CategoryCode.MEDICAL)),
                MerchantTypeCategoryMap.seed("이용,미용", categories.get(CategoryCode.BEAUTY)),
                MerchantTypeCategoryMap.seed("각종회비", categories.get(CategoryCode.DONATION)),
                MerchantTypeCategoryMap.seed("컴퓨터  소프트웨어", categories.get(CategoryCode.SUBSCRIPTION)),
                MerchantTypeCategoryMap.seed("화원", categories.get(CategoryCode.SHOPPING))
        ));
    }

    private void seedKeywordRules(Map<CategoryCode, Category> categories) {
        if (categoryKeywordRuleRepository.count() > 0) {
            return;
        }
        categoryKeywordRuleRepository.saveAll(List.of(
                CategoryKeywordRule.seed("쿠팡(쿠페이)", categories.get(CategoryCode.SUBSCRIPTION)),
                CategoryKeywordRule.seed("쿠팡이츠", categories.get(CategoryCode.DELIVERY)),
                CategoryKeywordRule.seed("쿠팡", categories.get(CategoryCode.SHOPPING)),
                CategoryKeywordRule.seed("쏘카", categories.get(CategoryCode.TRANSPORT)),
                CategoryKeywordRule.seed("카카오T", categories.get(CategoryCode.TRANSPORT)),
                CategoryKeywordRule.seed("카카오모빌리티", categories.get(CategoryCode.TRANSPORT)),
                CategoryKeywordRule.seed("나인투원(킥보드)", categories.get(CategoryCode.TRANSPORT)),
                CategoryKeywordRule.seed("피플카쉐어링", categories.get(CategoryCode.TRANSPORT)),
                CategoryKeywordRule.seed("ANTHROPIC", categories.get(CategoryCode.SUBSCRIPTION)),
                CategoryKeywordRule.seed("CLAUDE", categories.get(CategoryCode.SUBSCRIPTION)),
                CategoryKeywordRule.seed("구글클라우드", categories.get(CategoryCode.SUBSCRIPTION)),
                CategoryKeywordRule.seed("AWS", categories.get(CategoryCode.SUBSCRIPTION)),
                CategoryKeywordRule.seed("Amazon", categories.get(CategoryCode.SUBSCRIPTION)),
                CategoryKeywordRule.seed("PC CAFE", categories.get(CategoryCode.LEISURE)),
                CategoryKeywordRule.seed("긱스타", categories.get(CategoryCode.LEISURE)),
                CategoryKeywordRule.seed("의원", categories.get(CategoryCode.MEDICAL))
        ));
    }
}
