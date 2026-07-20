package com.weaone.themoa.domain.customerservice.service;

import com.weaone.themoa.common.exception.BusinessException;
import com.weaone.themoa.common.exception.ErrorCode;
import com.weaone.themoa.domain.customerservice.dto.request.InquiryAnswerRequest;
import com.weaone.themoa.domain.customerservice.dto.response.AdminInquiryDetailResponse;
import com.weaone.themoa.domain.customerservice.dto.response.AdminInquiryListItemResponse;
import com.weaone.themoa.domain.customerservice.dto.response.AdminInquiryListResponse;
import com.weaone.themoa.domain.customerservice.dto.response.AttachmentResponse;
import com.weaone.themoa.domain.customerservice.dto.response.InquiryAnswerResponse;
import com.weaone.themoa.domain.customerservice.entity.CustomerInquiry;
import com.weaone.themoa.domain.customerservice.entity.CustomerInquiryAnswer;
import com.weaone.themoa.domain.customerservice.entity.CustomerInquiryAttachment;
import com.weaone.themoa.domain.customerservice.entity.InquiryStatus;
import com.weaone.themoa.domain.customerservice.repository.CustomerInquiryAnswerRepository;
import com.weaone.themoa.domain.customerservice.repository.CustomerInquiryAttachmentRepository;
import com.weaone.themoa.domain.customerservice.repository.CustomerInquiryRepository;
import com.weaone.themoa.domain.customerservice.support.CustomerInquiryFileStorage;
import com.weaone.themoa.domain.customerservice.support.DownloadableAttachment;
import com.weaone.themoa.domain.customerservice.support.MarkdownValidator;
import com.weaone.themoa.domain.customerservice.support.StoredFile;
import com.weaone.themoa.domain.member.entity.Member;
import com.weaone.themoa.domain.member.repository.MemberRepository;
import com.weaone.themoa.domain.notification.entity.NotificationTypeCode;
import com.weaone.themoa.domain.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/** 관리자 1:1 문의 조회·답변(customerservice.md §4-3). {@code member.role=ADMIN}만 접근한다. */
@Service
@RequiredArgsConstructor
public class AdminInquiryService {

    private static final int DEFAULT_SIZE = 10;
    private static final int MAX_SIZE = 50;
    private static final int ANSWER_MAX_LENGTH = 20_000;

    private final CustomerInquiryRepository inquiryRepository;
    private final CustomerInquiryAttachmentRepository attachmentRepository;
    private final CustomerInquiryAnswerRepository answerRepository;
    private final MemberRepository memberRepository;
    private final CustomerInquiryFileStorage fileStorage;
    private final NotificationService notificationService;

    @Transactional(readOnly = true)
    public AdminInquiryListResponse list(InquiryStatus status, Long inquiryCategoryId, String keyword,
                                          Integer page, Integer size) {
        String normalizedKeyword = normalize(keyword);
        Page<CustomerInquiry> inquiries = inquiryRepository.searchForAdmin(status, inquiryCategoryId,
                normalizedKeyword, PageRequest.of(normalizePage(page), clampSize(size)));
        return AdminInquiryListResponse.from(inquiries.map(AdminInquiryListItemResponse::from));
    }

    @Transactional(readOnly = true)
    public AdminInquiryDetailResponse detail(Long inquiryId) {
        CustomerInquiry inquiry = inquiryRepository.findById(inquiryId)
                .orElseThrow(() -> new BusinessException(ErrorCode.CUSTOMER_INQUIRY_NOT_FOUND));
        List<AttachmentResponse> attachments = attachmentRepository.findByInquiry_IdOrderById(inquiryId).stream()
                .map(AttachmentResponse::from)
                .toList();
        InquiryAnswerResponse answer = answerRepository.findByInquiry_Id(inquiryId)
                .map(InquiryAnswerResponse::from)
                .orElse(null);
        return AdminInquiryDetailResponse.of(inquiry, attachments, answer);
    }

    @Transactional(readOnly = true)
    public DownloadableAttachment downloadAttachment(Long inquiryId, Long attachmentId) {
        if (!inquiryRepository.existsById(inquiryId)) {
            throw new BusinessException(ErrorCode.CUSTOMER_INQUIRY_NOT_FOUND);
        }
        CustomerInquiryAttachment attachment = attachmentRepository.findByIdAndInquiry_Id(attachmentId, inquiryId)
                .orElseThrow(() -> new BusinessException(ErrorCode.CUSTOMER_INQUIRY_ATTACHMENT_NOT_FOUND));
        StoredFile stored = fileStorage.load(attachment.getObjectKey());
        return new DownloadableAttachment(attachment.getOriginalFilename(), attachment.getContentType(), stored.content());
    }

    /**
     * 답변 최초 등록 또는 수정(customerservice.md §7). 최초 등록은 답변·상태·알림을 한 트랜잭션에서 함께
     * 저장하고, 수정은 요청 version이 현재 version과 같을 때만 갱신하며 알림을 다시 만들지 않는다.
     */
    @Transactional
    public InquiryAnswerResponse upsertAnswer(Long adminId, Long inquiryId, InquiryAnswerRequest request) {
        CustomerInquiry inquiry = inquiryRepository.findById(inquiryId)
                .orElseThrow(() -> new BusinessException(ErrorCode.CUSTOMER_INQUIRY_NOT_FOUND));
        String content = validateContent(request.contentMarkdown());
        LocalDateTime now = LocalDateTime.now();

        CustomerInquiryAnswer existing = answerRepository.findByInquiry_Id(inquiryId).orElse(null);
        if (existing == null) {
            return createAnswer(inquiry, adminId, content, now);
        }
        if (request.version() == null || !request.version().equals(existing.getVersion())) {
            throw new BusinessException(ErrorCode.CUSTOMER_INQUIRY_ANSWER_CONFLICT);
        }
        existing.updateContent(content, now);
        try {
            answerRepository.saveAndFlush(existing);
        } catch (ObjectOptimisticLockingFailureException e) {
            throw new BusinessException(ErrorCode.CUSTOMER_INQUIRY_ANSWER_CONFLICT);
        }
        return InquiryAnswerResponse.from(existing);
    }

    private InquiryAnswerResponse createAnswer(CustomerInquiry inquiry, Long adminId, String content, LocalDateTime now) {
        Member admin = memberRepository.getReferenceById(adminId);
        CustomerInquiryAnswer answer;
        try {
            answer = answerRepository.saveAndFlush(CustomerInquiryAnswer.create(inquiry, admin, content, now));
        } catch (DataIntegrityViolationException e) {
            // 동시에 다른 관리자가 먼저 최초 답변을 등록했다(inquiry_id UNIQUE 경합).
            throw new BusinessException(ErrorCode.CUSTOMER_INQUIRY_ANSWER_CONFLICT);
        }
        inquiry.markAnswered(now);
        notificationService.createIfAbsent(inquiry.getMember(), NotificationTypeCode.INQUIRY_ANSWERED,
                "문의에 답변이 등록되었습니다.", null, inquiry, "INQUIRY_ANSWERED:inquiry=" + inquiry.getId());
        return InquiryAnswerResponse.from(answer);
    }

    private String validateContent(String contentMarkdown) {
        String trimmed = contentMarkdown == null ? "" : contentMarkdown.trim();
        if (trimmed.isEmpty() || trimmed.length() > ANSWER_MAX_LENGTH) {
            throw new BusinessException(ErrorCode.CUSTOMER_INQUIRY_INVALID_REQUEST);
        }
        if (MarkdownValidator.containsRawHtml(trimmed)) {
            throw new BusinessException(ErrorCode.CUSTOMER_INQUIRY_INVALID_REQUEST);
        }
        return trimmed;
    }

    private String normalize(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim().toLowerCase();
        return trimmed.isEmpty() ? null : trimmed;
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
