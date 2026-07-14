package com.weaone.themoa.domain.cardtransaction.client;

/**
 * CODEF 카드 승인내역 조회 응답 1건(원문 필드 그대로, 파싱은 호출자 책임). 필드명은 CODEF 응답 키와 1:1이다.
 */
public record CodefApprovalRecord(
        String resUsedDate,
        String resUsedTime,
        String resCardNo,
        String resCardName,
        String resMemberStoreName,
        String resUsedAmount,
        String resAccountCurrency,
        String resApprovalNo,
        String resHomeForeignType,
        String resMemberStoreType,
        String resMemberStoreAddr,
        String resMemberStoreCorpNo,
        String resCancelYN,
        String resCancelAmount,
        String resKRWAmt,
        String resInstallmentMonth
) {
}
