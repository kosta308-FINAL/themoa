package com.weaone.themoa.domain.customerservice.support;

/** 다운로드 API 응답 조립용 첨부파일 바이트+메타데이터. */
public record DownloadableAttachment(String originalFilename, String contentType, byte[] content) {
}
