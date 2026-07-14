package com.weaone.themoa.domain.merchant.support;

import com.weaone.themoa.domain.category.entity.Category;
import com.weaone.themoa.domain.category.entity.CategoryCode;
import com.weaone.themoa.domain.category.repository.CategoryRepository;
import com.weaone.themoa.domain.merchant.entity.Biller;
import com.weaone.themoa.domain.merchant.entity.MerchantAlias;
import com.weaone.themoa.domain.merchant.entity.MerchantAliasTerms;
import com.weaone.themoa.domain.merchant.repository.BillerRepository;
import com.weaone.themoa.domain.merchant.repository.MerchantAliasRepository;
import com.weaone.themoa.domain.merchant.repository.MerchantAliasTermsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 이름형 구독 전역 시드(merchant_alias + merchant_alias_terms, member=NULL) + 결제대행자 화이트리스트
 * (merchant.md §5-A/B/D-1). 카테고리는 {@link CategoryCode}로 참조한다 —
 * {@link com.weaone.themoa.domain.category.support.CategorySeeder}가 먼저 실행돼 카테고리 마스터를
 * 채워 둬야 하므로 이 시더는 그보다 뒤에 실행된다({@link Order}).
 */
@Component
@Order(2)
@RequiredArgsConstructor
public class MerchantAliasSeeder implements ApplicationRunner {

    private final MerchantAliasRepository merchantAliasRepository;
    private final MerchantAliasTermsRepository merchantAliasTermsRepository;
    private final BillerRepository billerRepository;
    private final CategoryRepository categoryRepository;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        seedAliasesAndTerms();
        seedBillers();
    }

    private void seedAliasesAndTerms() {
        if (merchantAliasRepository.count() > 0) {
            return;
        }
        Category subscription = findCategory(CategoryCode.SUBSCRIPTION);
        Category donation = findCategory(CategoryCode.DONATION);
        MerchantAlias claude = merchantAliasRepository.save(MerchantAlias.create("Claude 구독", subscription));
        MerchantAlias coupangWow = merchantAliasRepository.save(MerchantAlias.create("쿠팡와우 멤버십", subscription));
        MerchantAlias redCross = merchantAliasRepository.save(MerchantAlias.create("대한적십자사 정기후원", donation));

        merchantAliasTermsRepository.saveAll(List.of(
                MerchantAliasTerms.seed(claude, "CLAUDE.AI SUBSCRIPTION"),
                MerchantAliasTerms.seed(claude, "ANTHROPIC* CLAUDE SUB"),
                MerchantAliasTerms.seed(coupangWow, "쿠팡(쿠페이)"),
                MerchantAliasTerms.seed(redCross, "대한적십자사")
        ));
    }

    private void seedBillers() {
        if (billerRepository.count() > 0) {
            return;
        }
        billerRepository.saveAll(List.of(
                Biller.seed("Apple"),
                Biller.seed("Google Play"),
                Biller.seed("구글페이먼트코리아")
        ));
    }

    private Category findCategory(CategoryCode code) {
        return categoryRepository.findByCode(code.name())
                .orElseThrow(() -> new IllegalStateException(code + " 카테고리가 시드되지 않았습니다."));
    }
}
