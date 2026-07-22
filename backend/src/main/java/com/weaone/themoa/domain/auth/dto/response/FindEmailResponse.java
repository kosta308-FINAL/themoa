package com.weaone.themoa.domain.auth.dto.response;

/** 실제 이메일 대신 마스킹된 값만 내려준다(예: so***@gmail.com). */
public record FindEmailResponse(String maskedEmail) {
}
