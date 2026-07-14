package com.weaone.themoa.domain.merchant.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class MerchantTest {

    @Test
    @DisplayName("전역 시드 매칭 결과가 있으면 관찰 즉시 alias가 연결된다")
    void observeLinksGlobalAliasWhenGiven() {
        MerchantAlias alias = MerchantAlias.create("Claude 구독", 7L);

        Merchant merchant = Merchant.observe("CLAUDE.AI SUBSCRIPTION", alias);

        assertThat(merchant.getMerchantNameRaw()).isEqualTo("CLAUDE.AI SUBSCRIPTION");
        assertThat(merchant.getMerchantAlias()).isEqualTo(alias);
    }

    @Test
    @DisplayName("전역 시드에 없는 원본명은 alias 없이 관찰된다")
    void observeWithoutAliasWhenNoGlobalMatch() {
        Merchant merchant = Merchant.observe("복뚱이네 간장게장", null);

        assertThat(merchant.getMerchantAlias()).isNull();
    }
}
