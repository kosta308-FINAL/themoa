package com.weaone.themoa.domain.merchant.support;

import com.weaone.themoa.domain.merchant.entity.Biller;
import com.weaone.themoa.domain.merchant.entity.MerchantAlias;
import com.weaone.themoa.domain.merchant.entity.MerchantAliasTerms;
import com.weaone.themoa.domain.merchant.repository.BillerRepository;
import com.weaone.themoa.domain.merchant.repository.MerchantAliasRepository;
import com.weaone.themoa.domain.merchant.repository.MerchantAliasTermsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 이름형 구독 전역 시드(merchant_alias + merchant_alias_terms, member=NULL) + 결제대행자 화이트리스트.
 * category_id는 category.md §3 확정값(구독=7, 기부/회비=11)을 그대로 쓴다. category 도메인이 아직
 * 없어 FK 연관관계 없이 값만 저장한다(merchant.md §5-A/B/D-1).
 */
@Component
@RequiredArgsConstructor
public class MerchantAliasSeeder implements ApplicationRunner {

    private static final Long CATEGORY_SUBSCRIPTION = 7L;
    private static final Long CATEGORY_DONATION = 11L;

    private final MerchantAliasRepository merchantAliasRepository;
    private final MerchantAliasTermsRepository merchantAliasTermsRepository;
    private final BillerRepository billerRepository;

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
        MerchantAlias claude = merchantAliasRepository.save(MerchantAlias.create("Claude 구독", CATEGORY_SUBSCRIPTION));
        MerchantAlias coupangWow = merchantAliasRepository.save(MerchantAlias.create("쿠팡와우 멤버십", CATEGORY_SUBSCRIPTION));
        MerchantAlias redCross = merchantAliasRepository.save(MerchantAlias.create("대한적십자사 정기후원", CATEGORY_DONATION));

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
}
