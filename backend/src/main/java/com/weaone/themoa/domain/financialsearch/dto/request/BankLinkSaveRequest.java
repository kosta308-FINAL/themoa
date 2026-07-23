package com.weaone.themoa.domain.financialsearch.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * 은행 공식 링크 등록·수정 요청. 회사명이 식별자라 같은 회사명으로 다시 저장하면 URL이 갱신된다.
 *
 * <p>회사명은 상품 데이터(savings_product/loan_product)의 company_name과 같은 문자열이어야 매칭된다.
 * URL은 사용자에게 그대로 노출되므로 형식을 검증한다 — 검증되지 않은 주소를 임의로 넣으면 안 된다.
 */
public record BankLinkSaveRequest(

        @NotBlank(message = "금융회사명을 입력해 주세요.")
        @Size(max = 100, message = "금융회사명은 100자 이하여야 합니다.")
        String companyName,

        @NotBlank(message = "공식 홈페이지 주소를 입력해 주세요.")
        @Size(max = 500, message = "주소는 500자 이하여야 합니다.")
        @Pattern(regexp = "^https?://.+", message = "http:// 또는 https:// 로 시작하는 주소여야 합니다.")
        String officialUrl
) {
}
