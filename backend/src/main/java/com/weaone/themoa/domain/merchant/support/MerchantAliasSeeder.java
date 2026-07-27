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

    /**
     * 없는 것만 추가한다(category.md의 CategorySeeder와 동일한 이유 — 이미 시드가 끝난 DB에도 새 alias·
     * 표기가 반영돼야 한다). ChatGPT 구독(OpenAI)은 Claude와 같은 패턴(전역 시드 alias + 자기 서비스명이
     * 드러나는 결제 표기)이라 2026-07 실데이터 분석으로 추가했다. 쿠팡와우 멤버십은 "쿠팡(쿠페이)" 외에
     * "쿠팡(와우 멤버십)"/"쿠팡(와우멤버십)" 표기 변형이 실데이터에서 추가로 확인돼 함께 등록한다.
     */
    private void seedAliasesAndTerms() {
        Category subscription = findCategory(CategoryCode.SUBSCRIPTION);
        Category donation = findCategory(CategoryCode.DONATION);

        MerchantAlias claude = findOrCreateAlias("Claude 구독", subscription);
        MerchantAlias coupangWow = findOrCreateAlias("쿠팡와우 멤버십", subscription);
        MerchantAlias redCross = findOrCreateAlias("대한적십자사 정기후원", donation);
        MerchantAlias chatGpt = findOrCreateAlias("ChatGPT 구독", subscription);

        seedTermIfAbsent(claude, "CLAUDE.AI SUBSCRIPTION");
        seedTermIfAbsent(claude, "ANTHROPIC* CLAUDE SUB");
        seedTermIfAbsent(coupangWow, "쿠팡(쿠페이)");
        seedTermIfAbsent(coupangWow, "쿠팡(와우 멤버십)");
        seedTermIfAbsent(coupangWow, "쿠팡(와우멤버십)");
        seedTermIfAbsent(redCross, "대한적십자사");
        seedTermIfAbsent(chatGpt, "OPENAI *CHATGPT SUBSCR");
    }

    private MerchantAlias findOrCreateAlias(String canonicalServiceName, Category defaultCategory) {
        return merchantAliasRepository.findByCanonicalServiceNameNormalized(canonicalServiceName)
                .orElseGet(() -> merchantAliasRepository.save(MerchantAlias.create(canonicalServiceName, defaultCategory)));
    }

    private void seedTermIfAbsent(MerchantAlias alias, String aliasText) {
        if (merchantAliasTermsRepository.findGlobalByRawName(aliasText).isEmpty()) {
            merchantAliasTermsRepository.save(MerchantAliasTerms.seed(alias, aliasText));
        }
    }

    /**
     * 없는 것만 추가한다. 비바리퍼블리카(토스페이먼츠)·NICE 결제대행은 Apple·Google Play와 같은 성격의
     * 결제대행자다 — 원본 가맹점명이 그 이름 하나로만 찍혀 나와서(2026-07 실데이터 확인) 이름만으로는
     * 어떤 실제 서비스인지 구분할 수 없다(merchant.md §5-D-1).
     */
    private void seedBillers() {
        List<String> desired = List.of(
                "Apple", "Google Play", "구글페이먼트코리아", "비바리퍼블리카", "NICE 결제대행");
        List<Biller> missing = desired.stream()
                .filter(name -> !billerRepository.existsByNameNormalized(name))
                .map(Biller::seed)
                .toList();
        if (!missing.isEmpty()) {
            billerRepository.saveAll(missing);
        }
    }

    private Category findCategory(CategoryCode code) {
        return categoryRepository.findByCode(code.name())
                .orElseThrow(() -> new IllegalStateException(code + " 카테고리가 시드되지 않았습니다."));
    }
}
