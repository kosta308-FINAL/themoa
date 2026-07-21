package com.weaone.themoa.domain.customerservice.service;

import com.weaone.themoa.common.exception.BusinessException;
import com.weaone.themoa.common.exception.ErrorCode;
import com.weaone.themoa.config.CustomerServiceProperties;
import com.weaone.themoa.domain.customerservice.dto.request.InquiryCreateRequest;
import com.weaone.themoa.domain.customerservice.dto.response.AttachmentResponse;
import com.weaone.themoa.domain.customerservice.dto.response.InquiryAnswerResponse;
import com.weaone.themoa.domain.customerservice.dto.response.InquiryCategoryResponse;
import com.weaone.themoa.domain.customerservice.dto.response.InquiryDetailResponse;
import com.weaone.themoa.domain.customerservice.dto.response.InquiryListItemResponse;
import com.weaone.themoa.domain.customerservice.dto.response.InquiryListResponse;
import com.weaone.themoa.domain.customerservice.entity.CustomerInquiry;
import com.weaone.themoa.domain.customerservice.entity.CustomerInquiryAttachment;
import com.weaone.themoa.domain.customerservice.entity.CustomerInquiryCategory;
import com.weaone.themoa.domain.customerservice.repository.CustomerInquiryAnswerRepository;
import com.weaone.themoa.domain.customerservice.repository.CustomerInquiryAttachmentRepository;
import com.weaone.themoa.domain.customerservice.repository.CustomerInquiryCategoryRepository;
import com.weaone.themoa.domain.customerservice.repository.CustomerInquiryRepository;
import com.weaone.themoa.domain.customerservice.support.CustomerInquiryFileStorage;
import com.weaone.themoa.domain.customerservice.support.DownloadableAttachment;
import com.weaone.themoa.domain.customerservice.support.ImageSignatureValidator;
import com.weaone.themoa.domain.customerservice.support.SafeFilename;
import com.weaone.themoa.domain.customerservice.support.StoredFile;
import com.weaone.themoa.domain.member.entity.Member;
import com.weaone.themoa.domain.member.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** 회원 1:1 문의 접수·조회(customerservice.md §4-2). */
@Service
@RequiredArgsConstructor
public class InquiryService {

    private static final int DEFAULT_SIZE = 10;
    private static final int MAX_SIZE = 50;
    private static final int MAX_FILE_COUNT = 3;
    private static final long MAX_FILE_SIZE_BYTES = 10L * 1024 * 1024;
    private static final long MAX_TOTAL_SIZE_BYTES = 30L * 1024 * 1024;
    private static final int TITLE_MAX_LENGTH = 200;
    private static final int CONTENT_MAX_LENGTH = 10_000;

    private final CustomerInquiryCategoryRepository inquiryCategoryRepository;
    private final CustomerInquiryRepository inquiryRepository;
    private final CustomerInquiryAttachmentRepository attachmentRepository;
    private final CustomerInquiryAnswerRepository answerRepository;
    private final MemberRepository memberRepository;
    private final CustomerInquiryFileStorage fileStorage;
    private final CustomerServiceProperties properties;

    @Transactional(readOnly = true)
    public List<InquiryCategoryResponse> listCategories() {
        return inquiryCategoryRepository.findByActiveTrueOrderByDisplayOrderAscIdAsc().stream()
                .map(InquiryCategoryResponse::from)
                .toList();
    }

    @Transactional
    public InquiryDetailResponse create(Long memberId, InquiryCreateRequest request, List<MultipartFile> files) {
        List<MultipartFile> safeFiles = files == null ? List.of() : files;
        String title = requireLength(request.title(), 1, TITLE_MAX_LENGTH);
        String content = requireLength(request.content(), 1, CONTENT_MAX_LENGTH);
        if (!Boolean.TRUE.equals(request.agreedPrivacy())) {
            throw new BusinessException(ErrorCode.CUSTOMER_INQUIRY_PRIVACY_REQUIRED);
        }
        CustomerInquiryCategory category = inquiryCategoryRepository.findByIdAndActiveTrue(request.inquiryCategoryId())
                .orElseThrow(() -> new BusinessException(ErrorCode.CUSTOMER_INQUIRY_INVALID_REQUEST));
        validateFiles(safeFiles);

        Member member = memberRepository.getReferenceById(memberId);
        LocalDateTime now = LocalDateTime.now();
        CustomerInquiry inquiry = inquiryRepository.save(
                CustomerInquiry.create(member, category, title, content, properties.privacyPolicyVersion(), now));

        List<String> storedKeys = new ArrayList<>();
        try {
            for (MultipartFile file : safeFiles) {
                byte[] bytes = readBytes(file);
                String contentType = ImageSignatureValidator.detectContentType(bytes);
                if (contentType == null) {
                    throw new BusinessException(ErrorCode.CUSTOMER_INQUIRY_FILE_TYPE_NOT_ALLOWED);
                }
                String objectKey = buildObjectKey(memberId, inquiry.getId(), contentType);
                fileStorage.store(objectKey, bytes, contentType);
                storedKeys.add(objectKey);
                attachmentRepository.save(CustomerInquiryAttachment.create(
                        inquiry, objectKey, SafeFilename.sanitize(file.getOriginalFilename()),
                        bytes.length, contentType, now));
            }
            attachmentRepository.flush();
        } catch (RuntimeException e) {
            for (String key : storedKeys) {
                fileStorage.delete(key);
            }
            if (e instanceof BusinessException) {
                throw e;
            }
            throw new BusinessException(ErrorCode.CUSTOMER_INQUIRY_FILE_STORAGE_FAILED);
        }

        List<AttachmentResponse> attachments = attachmentRepository.findByInquiry_IdOrderById(inquiry.getId()).stream()
                .map(AttachmentResponse::from)
                .toList();
        return InquiryDetailResponse.of(inquiry, attachments, null);
    }

    @Transactional(readOnly = true)
    public InquiryListResponse list(Long memberId, Integer page, Integer size) {
        Page<CustomerInquiry> inquiries = inquiryRepository.findByMember_IdOrderByCreatedAtDescIdDesc(
                memberId, PageRequest.of(normalizePage(page), clampSize(size)));
        return InquiryListResponse.from(inquiries.map(InquiryListItemResponse::from));
    }

    @Transactional(readOnly = true)
    public InquiryDetailResponse detail(Long memberId, Long inquiryId) {
        CustomerInquiry inquiry = inquiryRepository.findByIdAndMember_Id(inquiryId, memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.CUSTOMER_INQUIRY_NOT_FOUND));
        List<AttachmentResponse> attachments = attachmentRepository.findByInquiry_IdOrderById(inquiryId).stream()
                .map(AttachmentResponse::from)
                .toList();
        InquiryAnswerResponse answer = answerRepository.findByInquiry_Id(inquiryId)
                .map(InquiryAnswerResponse::from)
                .orElse(null);
        return InquiryDetailResponse.of(inquiry, attachments, answer);
    }

    @Transactional(readOnly = true)
    public DownloadableAttachment downloadAttachment(Long memberId, Long inquiryId, Long attachmentId) {
        inquiryRepository.findByIdAndMember_Id(inquiryId, memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.CUSTOMER_INQUIRY_NOT_FOUND));
        CustomerInquiryAttachment attachment = attachmentRepository.findByIdAndInquiry_Id(attachmentId, inquiryId)
                .orElseThrow(() -> new BusinessException(ErrorCode.CUSTOMER_INQUIRY_ATTACHMENT_NOT_FOUND));
        StoredFile stored = fileStorage.load(attachment.getObjectKey());
        return new DownloadableAttachment(attachment.getOriginalFilename(), attachment.getContentType(), stored.content());
    }

    private void validateFiles(List<MultipartFile> files) {
        if (files.size() > MAX_FILE_COUNT) {
            throw new BusinessException(ErrorCode.CUSTOMER_INQUIRY_FILE_LIMIT_EXCEEDED);
        }
        long total = 0;
        for (MultipartFile file : files) {
            if (file.getSize() > MAX_FILE_SIZE_BYTES) {
                throw new BusinessException(ErrorCode.CUSTOMER_INQUIRY_FILE_LIMIT_EXCEEDED);
            }
            total += file.getSize();
        }
        if (total > MAX_TOTAL_SIZE_BYTES) {
            throw new BusinessException(ErrorCode.CUSTOMER_INQUIRY_FILE_LIMIT_EXCEEDED);
        }
    }

    private byte[] readBytes(MultipartFile file) {
        try {
            return file.getBytes();
        } catch (IOException e) {
            throw new BusinessException(ErrorCode.CUSTOMER_INQUIRY_FILE_STORAGE_FAILED);
        }
    }

    private String buildObjectKey(Long memberId, Long inquiryId, String contentType) {
        String extension = ImageSignatureValidator.extensionFor(contentType);
        return "members/" + memberId + "/inquiries/" + inquiryId + "/" + UUID.randomUUID() + "." + extension;
    }

    private String requireLength(String value, int min, int max) {
        String trimmed = value == null ? "" : value.trim();
        if (trimmed.length() < min || trimmed.length() > max) {
            throw new BusinessException(ErrorCode.CUSTOMER_INQUIRY_INVALID_REQUEST);
        }
        return trimmed;
    }

    private int normalizePage(Integer page) {
        return page == null || page < 0 ? 0 : page;
    }

    private int clampSize(Integer size) {
        if (size == null || size <= 0) {
            return DEFAULT_SIZE;
        }
        return Math.min(size, MAX_SIZE);
    }
}
