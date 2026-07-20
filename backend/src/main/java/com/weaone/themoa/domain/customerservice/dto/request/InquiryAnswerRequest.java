package com.weaone.themoa.domain.customerservice.dto.request;

/** 답변 최초 등록·수정 공용 요청(customerservice.md §4-3). 최초 등록은 version을 생략하거나 null로 보낸다. */
public record InquiryAnswerRequest(String contentMarkdown, Long version) {
}
