package com.weaone.themoa.domain.customerservice.support;

/** 저장소에서 읽어온 첨부파일 바이트와 콘텐츠 타입. */
public record StoredFile(byte[] content, String contentType) {
}
