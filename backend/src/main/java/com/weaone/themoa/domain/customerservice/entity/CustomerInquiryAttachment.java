package com.weaone.themoa.domain.customerservice.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.time.LocalDateTime;

/**
 * 1:1 문의 첨부파일 메타데이터(erd.md §8). 실제 바이트는 {@code object_key}가 가리키는 로컬/S3 저장소에
 * 있고, DB에는 절대경로나 공개 URL을 두지 않는다(customerservice.md §6-1).
 */
@Entity
@Table(name = "customer_inquiry_attachment",
        indexes = @Index(name = "idx_customer_inquiry_attachment_inquiry", columnList = "inquiry_id, id"))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CustomerInquiryAttachment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "inquiry_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private CustomerInquiry inquiry;

    @Column(name = "object_key", nullable = false, unique = true, length = 500)
    private String objectKey;

    @Column(name = "original_filename", nullable = false, length = 255)
    private String originalFilename;

    @Column(name = "file_size", nullable = false)
    private long fileSize;

    @Column(name = "content_type", nullable = false, length = 100)
    private String contentType;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    private CustomerInquiryAttachment(CustomerInquiry inquiry, String objectKey, String originalFilename,
                                       long fileSize, String contentType, LocalDateTime now) {
        this.inquiry = inquiry;
        this.objectKey = objectKey;
        this.originalFilename = originalFilename;
        this.fileSize = fileSize;
        this.contentType = contentType;
        this.createdAt = now;
    }

    public static CustomerInquiryAttachment create(CustomerInquiry inquiry, String objectKey,
                                                     String originalFilename, long fileSize, String contentType,
                                                     LocalDateTime now) {
        return new CustomerInquiryAttachment(inquiry, objectKey, originalFilename, fileSize, contentType, now);
    }
}
