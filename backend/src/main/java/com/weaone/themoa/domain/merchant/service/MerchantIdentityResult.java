package com.weaone.themoa.domain.merchant.service;

/**
 * 가맹점 신원 판별 결과. {@code merchantAliasId}는 해석 실패 시 NULL이다(추측 금지, merchant.md §2-1).
 * {@code biller}가 true면 이름으로는 신원 판별이 안 되는 결제대행 경유 거래라는 뜻이며,
 * 이 경우 호출자(향후 fixedExpense.md)가 금액·주기로 별도 매칭해야 한다(merchant.md §5-D-2).
 */
public record MerchantIdentityResult(Long merchantId, Long merchantAliasId, boolean biller) {

    public static MerchantIdentityResult identified(Long merchantId, Long merchantAliasId) {
        return new MerchantIdentityResult(merchantId, merchantAliasId, false);
    }

    public static MerchantIdentityResult biller(Long merchantId) {
        return new MerchantIdentityResult(merchantId, null, true);
    }
}
