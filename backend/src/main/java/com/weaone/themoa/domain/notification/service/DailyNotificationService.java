package com.weaone.themoa.domain.notification.service;

import com.weaone.themoa.common.exception.BusinessException;
import com.weaone.themoa.common.exception.ErrorCode;
import com.weaone.themoa.domain.budget.service.BudgetCycleService;
import com.weaone.themoa.domain.calendar.dto.response.CalendarEventListResponse;
import com.weaone.themoa.domain.calendar.dto.response.CalendarEventResponse;
import com.weaone.themoa.domain.calendar.entity.CalendarEventType;
import com.weaone.themoa.domain.calendar.service.CalendarQueryService;
import com.weaone.themoa.domain.datarefresh.entity.DataRefreshSource;
import com.weaone.themoa.domain.datarefresh.service.DataRefreshStatusService;
import com.weaone.themoa.domain.fixedexpense.entity.FixedExpense;
import com.weaone.themoa.domain.fixedexpense.repository.FixedExpenseRepository;
import com.weaone.themoa.domain.member.entity.Member;
import com.weaone.themoa.domain.member.repository.MemberRepository;
import com.weaone.themoa.domain.notification.dto.response.NotificationListResponse;
import com.weaone.themoa.domain.notification.entity.NotificationTypeCode;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;

@Service
@RequiredArgsConstructor
public class DailyNotificationService {

    private static final ZoneId SEOUL_ZONE = ZoneId.of("Asia/Seoul");

    private final MemberRepository memberRepository;
    private final CalendarQueryService calendarQueryService;
    private final FixedExpenseRepository fixedExpenseRepository;
    private final BudgetCycleService budgetCycleService;
    private final NotificationService notificationService;
    private final NotificationQueryService notificationQueryService;
    private final DataRefreshStatusService dataRefreshStatusService;

    @Transactional
    public NotificationListResponse prepareAndList(Long memberId, Pageable pageable) {
        Member member = memberRepository.findByIdForUpdate(memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));
        LocalDate today = LocalDate.now(SEOUL_ZONE);

        prepareCalendarReminders(member, today);
        prepareContentUpdated(member, today);

        return notificationQueryService.list(memberId, pageable);
    }

    private void prepareCalendarReminders(Member member, LocalDate today) {
        CalendarEventListResponse events = calendarQueryService.getEvents(member.getId(), today, today.plusDays(2));
        for (CalendarEventResponse event : events.items()) {
            long daysUntil = ChronoUnit.DAYS.between(today, event.eventDate());
            if (daysUntil < 0 || daysUntil > 2) {
                continue;
            }
            if (event.eventType() == CalendarEventType.FIXED_EXPENSE_DUE) {
                prepareFixedExpenseReminder(member, event, today, daysUntil);
                continue;
            }
            createCalendarReminder(member, event, today, daysUntil, null);
        }
    }

    private void prepareFixedExpenseReminder(Member member, CalendarEventResponse event, LocalDate today,
                                             long daysUntil) {
        if (event.sourceId() == null) {
            return;
        }
        FixedExpense fixedExpense = fixedExpenseRepository.findByIdAndMember_Id(event.sourceId(), member.getId())
                .orElse(null);
        if (fixedExpense == null) {
            return;
        }
        if (daysUntil == 0) {
            budgetCycleService.ensurePaydayPromoted(member, today);
            String yearMonth = budgetCycleService.resolveCycleForDate(member, today).yearMonth();
            String dedupKey = "PAYMENT_DUE:fe=" + fixedExpense.getId() + ":" + yearMonth;
            String message = "오늘 " + fixedExpense.getName() + " 결제일이에요.";
            notificationService.createIfAbsent(member, NotificationTypeCode.PAYMENT_DUE, message, fixedExpense, null,
                    dedupKey);
            return;
        }
        createCalendarReminder(member, event, today, daysUntil, fixedExpense);
    }

    private void createCalendarReminder(Member member, CalendarEventResponse event, LocalDate today, long daysUntil,
                                        FixedExpense fixedExpense) {
        String dedupKey = "CALENDAR_REMINDER:event=" + event.eventKey() + ":noticeDate=" + today;
        String message = calendarReminderMessage(event, daysUntil);
        notificationService.createIfAbsent(member, NotificationTypeCode.CALENDAR_REMINDER, message, fixedExpense, null,
                dedupKey);
    }

    private String calendarReminderMessage(CalendarEventResponse event, long daysUntil) {
        return "\"" + event.title() + "\" " + calendarEventLabel(event.eventType()) + dueText(daysUntil);
    }

    private String calendarEventLabel(CalendarEventType eventType) {
        return switch (eventType) {
            case USER_SCHEDULE -> "일정이 ";
            case FIXED_EXPENSE_DUE -> "결제일이 ";
            case POLICY_START -> "신청 시작이 ";
            case POLICY_DEADLINE -> "신청 마감이 ";
            case POLICY_SINGLE_DAY -> "신청일이 ";
        };
    }

    private String dueText(long daysUntil) {
        if (daysUntil == 2) {
            return "이틀 뒤예요.";
        }
        if (daysUntil == 1) {
            return "내일이에요.";
        }
        return "오늘이에요.";
    }

    private void prepareContentUpdated(Member member, LocalDate today) {
        boolean policyUpdated = dataRefreshStatusService.wasSuccessfulOn(DataRefreshSource.POLICY, today);
        boolean financialUpdated = dataRefreshStatusService.wasSuccessfulOn(DataRefreshSource.FINANCIAL, today);
        if (!policyUpdated || !financialUpdated) {
            return;
        }
        String dedupKey = "CONTENT_UPDATED:date=" + today;
        notificationService.createIfAbsent(
                member,
                NotificationTypeCode.CONTENT_UPDATED,
                "정책 및 금융상품 정보가 최신화됐어요.",
                null,
                null,
                dedupKey
        );
    }
}
