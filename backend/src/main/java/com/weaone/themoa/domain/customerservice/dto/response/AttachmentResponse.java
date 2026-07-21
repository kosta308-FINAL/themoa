package com.weaone.themoa.domain.customerservice.dto.response;

import com.weaone.themoa.domain.customerservice.entity.CustomerInquiryAttachment;

/** object_key는 응답에 절대 노출하지 않는다(customerservice.md §6-1). */
public record AttachmentResponse(Long id, String originalFilename, long fileSize, String contentType) {

    public static AttachmentResponse from(CustomerInquiryAttachment attachment) {
        return new AttachmentResponse(
                attachment.getId(),
                attachment.getOriginalFilename(),
                attachment.getFileSize(),
                attachment.getContentType()
        );
    }
}
