package com.weaone.themoa.domain.cardconnection.client;

/** CODEF 계정 등록(connectedId 발급) 요청 입력. 원문 자격증명은 클라이언트 안에서만 잠깐 존재하고 저장되지 않는다. */
public record CodefCreateAccountCommand(
        String organization,
        String loginId,
        String loginPassword,
        String cardNo,
        String cardPassword,
        String birthDate
) {
}
