package com.weaone.themoa.domain.cardconnection.dto.request;

import jakarta.validation.constraints.NotBlank;

/**
 * 현대(0302)는 cardNo·cardPassword가 항상 필수다(connection.md §3-1).
 * cardNo·cardPassword(KB 카드소지확인 케이스)·birthDate(우리카드 제한직전 케이스)는 그 외 카드사에서는 보통 비워 두고,
 * CODEF가 추가 입력을 요구하는 응답을 준 뒤 값을 채워 재요청하는 용도다(§3, §5-3).
 */
public record CardConnectionCreateRequest(
        @NotBlank String organization,
        @NotBlank String loginId,
        @NotBlank String loginPassword,
        String cardNo,
        String cardPassword,
        String birthDate
) {
}
