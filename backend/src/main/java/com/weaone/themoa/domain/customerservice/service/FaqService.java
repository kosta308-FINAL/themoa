package com.weaone.themoa.domain.customerservice.service;

import com.weaone.themoa.common.exception.BusinessException;
import com.weaone.themoa.common.exception.ErrorCode;
import com.weaone.themoa.domain.customerservice.dto.request.FaqFeedbackRequest;
import com.weaone.themoa.domain.customerservice.dto.response.FaqFeedbackCountRow;
import com.weaone.themoa.domain.customerservice.dto.response.FaqListResponse;
import com.weaone.themoa.domain.customerservice.dto.response.FaqResponse;
import com.weaone.themoa.domain.customerservice.entity.Faq;
import com.weaone.themoa.domain.customerservice.entity.FaqFeedback;
import com.weaone.themoa.domain.customerservice.repository.FaqFeedbackRepository;
import com.weaone.themoa.domain.customerservice.repository.FaqRepository;
import com.weaone.themoa.domain.member.entity.Member;
import com.weaone.themoa.domain.member.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** FAQ 조회·피드백(customerservice.md §4-1). */
@Service
@RequiredArgsConstructor
public class FaqService {

    private static final int DEFAULT_SIZE = 20;
    private static final int MAX_SIZE = 100;

    private final FaqRepository faqRepository;
    private final FaqFeedbackRepository faqFeedbackRepository;
    private final MemberRepository memberRepository;

    @Transactional(readOnly = true)
    public FaqListResponse search(Long memberId, String categoryCode, String keyword, Integer page, Integer size) {
        Pageable pageable = PageRequest.of(
                page == null || page < 0 ? 0 : page,
                clampSize(size)
        );
        String normalizedCategoryCode = normalize(categoryCode);
        String normalizedKeyword = keyword == null ? null : normalize(keyword.toLowerCase());

        Page<Faq> faqPage = faqRepository.search(normalizedCategoryCode, normalizedKeyword, pageable);
        List<Long> faqIds = faqPage.getContent().stream().map(Faq::getId).toList();

        Map<Long, long[]> counts = aggregateCounts(faqIds); // [helpful, unhelpful]
        Map<Long, Boolean> myFeedback = memberId == null ? Map.of() : loadMyFeedback(faqIds, memberId);

        Page<FaqResponse> responsePage = faqPage.map(faq -> {
            long[] count = counts.getOrDefault(faq.getId(), new long[2]);
            return FaqResponse.of(faq, count[0], count[1], myFeedback.get(faq.getId()));
        });
        return FaqListResponse.from(responsePage);
    }

    /** 멱등 API: 같은 값 재요청도 200으로 처리한다(customerservice.md §4-1). */
    @Transactional
    public void putFeedback(Long memberId, Long faqId, FaqFeedbackRequest request) {
        Faq faq = faqRepository.findByIdAndActiveTrue(faqId)
                .orElseThrow(() -> new BusinessException(ErrorCode.FAQ_NOT_FOUND));
        LocalDateTime now = LocalDateTime.now();

        FaqFeedback existing = faqFeedbackRepository.findByFaq_IdAndMember_Id(faqId, memberId).orElse(null);
        if (existing != null) {
            existing.changeHelpful(request.helpful(), now);
            return;
        }

        Member member = memberRepository.getReferenceById(memberId);
        try {
            faqFeedbackRepository.save(FaqFeedback.create(faq, member, request.helpful(), now));
        } catch (DataIntegrityViolationException e) {
            // 동시 최초 요청 경합: 이미 생긴 행을 재조회해 요청값으로 갱신한다(customerservice.md §8).
            faqFeedbackRepository.findByFaq_IdAndMember_Id(faqId, memberId)
                    .ifPresent(feedback -> feedback.changeHelpful(request.helpful(), now));
        }
    }

    private Map<Long, long[]> aggregateCounts(List<Long> faqIds) {
        Map<Long, long[]> result = new HashMap<>();
        if (faqIds.isEmpty()) {
            return result;
        }
        for (FaqFeedbackCountRow row : faqFeedbackRepository.countByFaqIds(faqIds)) {
            long[] count = result.computeIfAbsent(row.faqId(), id -> new long[2]);
            count[row.helpful() ? 0 : 1] = row.count();
        }
        return result;
    }

    private Map<Long, Boolean> loadMyFeedback(List<Long> faqIds, Long memberId) {
        if (faqIds.isEmpty()) {
            return Map.of();
        }
        Map<Long, Boolean> result = new HashMap<>();
        for (FaqFeedback feedback : faqFeedbackRepository.findByFaq_IdInAndMember_Id(faqIds, memberId)) {
            result.put(feedback.getFaq().getId(), feedback.isHelpful());
        }
        return result;
    }

    private int clampSize(Integer size) {
        if (size == null || size <= 0) {
            return DEFAULT_SIZE;
        }
        return Math.min(size, MAX_SIZE);
    }

    private String normalize(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
