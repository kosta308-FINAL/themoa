package com.weaone.themoa.domain.customerservice.support;

/**
 * 문의 첨부파일 저장소 계약(customerservice.md §6-1). 로컬·S3 구현체가 같은 object key 형식
 * ({@code members/{memberId}/inquiries/{inquiryId}/{uuid}.{png|jpg}})을 사용하고, DB에는 이 키만
 * 저장한다. 실제로 사용하는 구현체는 {@code app.customer-service.storage.provider} 설정으로 결정된다.
 */
public interface CustomerInquiryFileStorage {

    /** content 검증(크기·형식)은 호출자가 먼저 끝낸 뒤 호출한다. 실패 시 {@code CUSTOMER_INQUIRY_FILE_STORAGE_FAILED}로 변환한다. */
    void store(String objectKey, byte[] content, String contentType);

    /** 존재하지 않는 objectKey를 요청하면 {@code CUSTOMER_INQUIRY_FILE_STORAGE_FAILED}로 변환한다. */
    StoredFile load(String objectKey);

    /** 보상 삭제 전용(customerservice.md §6-4). 실패해도 예외를 던지지 않고 경고만 남긴다. */
    void delete(String objectKey);
}
