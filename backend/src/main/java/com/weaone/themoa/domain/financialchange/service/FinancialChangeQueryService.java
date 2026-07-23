package com.weaone.themoa.domain.financialchange.service;

import com.weaone.themoa.common.exception.BusinessException;
import com.weaone.themoa.common.exception.ErrorCode;
import com.weaone.themoa.domain.financialchange.dto.response.FinancialChangeResponse;
import com.weaone.themoa.domain.financialchange.entity.FinancialChangeNotice;
import com.weaone.themoa.domain.financialchange.repository.FinancialChangeNoticeRepository;
import com.weaone.themoa.domain.financialchange.repository.FinancialChangeNotificationRepository;
import com.weaone.themoa.domain.notification.entity.Notification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;

/**
 * 변경 내역 조회. 알림을 눌렀을 때 팝업에 띄울 "이전 → 이후"를 돌려준다.
 *
 * <p>알림 테이블에 우리 FK를 추가하지 않았으므로, 알림 id로 dedupKey를 찾은 뒤 그 키로 변경 내역을
 * 역추적한다.
 */
@Service
public class FinancialChangeQueryService {

    private final FinancialChangeNoticeRepository noticeRepository;
    private final FinancialChangeNotificationRepository notificationRepository;

    public FinancialChangeQueryService(FinancialChangeNoticeRepository noticeRepository,
                                       FinancialChangeNotificationRepository notificationRepository) {
        this.noticeRepository = noticeRepository;
        this.notificationRepository = notificationRepository;
    }

    /** 알림 id로 변경 상세를 찾는다(본인 알림만 조회된다). */
    @Transactional(readOnly = true)
    public FinancialChangeResponse findByNotification(Long memberId, Long notificationId) {
        Notification notification = notificationRepository.findByIdAndMember_Id(notificationId, memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOTIFICATION_NOT_FOUND));
        FinancialChangeNotice notice = noticeRepository
                .findByMemberIdAndDedupKey(memberId, notification.getDedupKey())
                .orElseThrow(() -> new BusinessException(ErrorCode.NOTIFICATION_NOT_FOUND));
        return toResponse(notice);
    }

    /** 회원의 변경 내역 목록(최근 순). 알림과 별개로 마이페이지 등에서 모아 볼 때 쓴다. */
    @Transactional(readOnly = true)
    public List<FinancialChangeResponse> findAll(Long memberId) {
        return noticeRepository.findByMemberIdOrderByCreatedAtDesc(memberId).stream()
                .map(this::toResponse)
                .toList();
    }

    private FinancialChangeResponse toResponse(FinancialChangeNotice notice) {
        List<FinancialChangeResponse.HistoryItem> history = noticeRepository
                .findTop10ByMemberIdAndTargetTypeAndTargetIdOrderByCreatedAtDesc(
                        notice.getMemberId(), notice.getTargetType(), notice.getTargetId())
                .stream()
                .map(row -> new FinancialChangeResponse.HistoryItem(
                        row.getPreviousRate(), row.getCurrentRate(), row.isDiscontinued(), row.getCreatedAt()))
                .toList();

        return new FinancialChangeResponse(
                notice.getId(),
                notice.getTargetType(),
                notice.getTargetId(),
                notice.getProductName(),
                notice.getCompanyName(),
                notice.getPreviousRate(),
                notice.getCurrentRate(),
                !equalsRate(notice.getPreviousRate(), notice.getCurrentRate()),
                notice.getPreviousSpecialCondition(),
                notice.getCurrentSpecialCondition(),
                !Objects.equals(nullToEmpty(notice.getPreviousSpecialCondition()),
                        nullToEmpty(notice.getCurrentSpecialCondition())),
                notice.isDiscontinued(),
                notice.getCreatedAt(),
                history);
    }

    private boolean equalsRate(BigDecimal a, BigDecimal b) {
        if (a == null || b == null) {
            return a == b;
        }
        return a.compareTo(b) == 0;
    }

    private String nullToEmpty(String value) {
        return value == null ? "" : value;
    }
}
