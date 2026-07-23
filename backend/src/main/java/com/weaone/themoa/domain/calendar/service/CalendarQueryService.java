package com.weaone.themoa.domain.calendar.service;

import com.weaone.themoa.common.exception.BusinessException;
import com.weaone.themoa.common.exception.ErrorCode;
import com.weaone.themoa.domain.calendar.dto.response.CalendarEventListResponse;
import com.weaone.themoa.domain.calendar.dto.response.CalendarEventResponse;
import com.weaone.themoa.domain.calendar.entity.CalendarEventType;
import com.weaone.themoa.domain.calendar.entity.CalendarSchedule;
import com.weaone.themoa.domain.calendar.repository.CalendarScheduleRepository;
import com.weaone.themoa.domain.fixedexpense.entity.FixedExpense;
import com.weaone.themoa.domain.fixedexpense.entity.FixedExpenseStatus;
import com.weaone.themoa.domain.fixedexpense.repository.FixedExpenseRepository;
import com.weaone.themoa.domain.policy.bookmark.entity.PolicyBookmark;
import com.weaone.themoa.domain.policy.bookmark.repository.PolicyBookmarkRepository;
import com.weaone.themoa.domain.policy.policy.entity.Policy;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CalendarQueryService {
    private static final long MAX_INCLUSIVE_DAYS = 62;

    private final CalendarScheduleRepository scheduleRepository;
    private final FixedExpenseRepository fixedExpenseRepository;
    private final PolicyBookmarkRepository policyBookmarkRepository;

    @Transactional(readOnly = true)
    public CalendarEventListResponse getEvents(Long memberId, LocalDate startDate, LocalDate endDate) {
        validateRange(startDate, endDate);
        List<CalendarEventResponse> events = new ArrayList<>();
        events.addAll(buildScheduleEvents(memberId, startDate, endDate));
        events.addAll(buildFixedExpenseEvents(memberId, startDate, endDate));
        events.addAll(buildPolicyEvents(memberId, startDate, endDate));
        events.sort(eventComparator());
        return new CalendarEventListResponse(startDate, endDate, events);
    }

    private void validateRange(LocalDate startDate, LocalDate endDate) {
        if (startDate == null || endDate == null || startDate.isAfter(endDate)) {
            throw new BusinessException(ErrorCode.CALENDAR_DATE_RANGE_INVALID);
        }
        long inclusiveDays = ChronoUnit.DAYS.between(startDate, endDate) + 1;
        if (inclusiveDays > MAX_INCLUSIVE_DAYS) {
            throw new BusinessException(ErrorCode.CALENDAR_DATE_RANGE_INVALID);
        }
    }

    private List<CalendarEventResponse> buildScheduleEvents(Long memberId, LocalDate startDate, LocalDate endDate) {
        return scheduleRepository
                .findByMember_IdAndScheduleDateBetweenOrderByScheduleDateAscIdAsc(memberId, startDate, endDate)
                .stream()
                .map(this::scheduleEvent)
                .toList();
    }

    private CalendarEventResponse scheduleEvent(CalendarSchedule schedule) {
        return new CalendarEventResponse(
                "USER_SCHEDULE:" + schedule.getId(),
                CalendarEventType.USER_SCHEDULE,
                schedule.getScheduleDate(),
                schedule.getTitle(),
                null,
                schedule.getId(),
                true
        );
    }

    private List<CalendarEventResponse> buildFixedExpenseEvents(Long memberId, LocalDate startDate, LocalDate endDate) {
        List<FixedExpense> fixedExpenses = fixedExpenseRepository
                .findByMember_IdAndStatusAndExpectedPayDayIsNotNull(memberId, FixedExpenseStatus.ACTIVE);
        List<CalendarEventResponse> events = new ArrayList<>();
        YearMonth cursor = YearMonth.from(startDate);
        YearMonth last = YearMonth.from(endDate);
        while (!cursor.isAfter(last)) {
            for (FixedExpense fixedExpense : fixedExpenses) {
                LocalDate eventDate = fixedExpenseDueDate(fixedExpense, cursor);
                if (!eventDate.isBefore(startDate) && !eventDate.isAfter(endDate)) {
                    events.add(fixedExpenseEvent(fixedExpense, eventDate));
                }
            }
            cursor = cursor.plusMonths(1);
        }
        return events;
    }

    private LocalDate fixedExpenseDueDate(FixedExpense fixedExpense, YearMonth yearMonth) {
        int day = Math.min(fixedExpense.getExpectedPayDay(), yearMonth.lengthOfMonth());
        return yearMonth.atDay(day);
    }

    private CalendarEventResponse fixedExpenseEvent(FixedExpense fixedExpense, LocalDate eventDate) {
        return new CalendarEventResponse(
                "FIXED_EXPENSE_DUE:" + fixedExpense.getId() + ":" + eventDate,
                CalendarEventType.FIXED_EXPENSE_DUE,
                eventDate,
                fixedExpense.getName(),
                fixedExpense.getExpectedAmountKrw(),
                fixedExpense.getId(),
                false
        );
    }

    private List<CalendarEventResponse> buildPolicyEvents(Long memberId, LocalDate startDate, LocalDate endDate) {
        List<PolicyBookmark> bookmarks = policyBookmarkRepository.findCalendarTargets(memberId, startDate, endDate);
        List<CalendarEventResponse> events = new ArrayList<>();
        for (PolicyBookmark bookmark : bookmarks) {
            Policy policy = bookmark.getPolicy();
            if (policy.isAlwaysOpen()) {
                continue;
            }
            LocalDate policyStartDate = policy.getStartDate();
            LocalDate dueDate = policy.getDueDate();
            if (policyStartDate != null && policyStartDate.equals(dueDate)) {
                if (isInRange(policyStartDate, startDate, endDate)) {
                    events.add(policyEvent(CalendarEventType.POLICY_SINGLE_DAY, policy, policyStartDate));
                }
                continue;
            }
            if (isInRange(policyStartDate, startDate, endDate)) {
                events.add(policyEvent(CalendarEventType.POLICY_START, policy, policyStartDate));
            }
            if (isInRange(dueDate, startDate, endDate)) {
                events.add(policyEvent(CalendarEventType.POLICY_DEADLINE, policy, dueDate));
            }
        }
        return events;
    }

    private boolean isInRange(LocalDate date, LocalDate startDate, LocalDate endDate) {
        return date != null && !date.isBefore(startDate) && !date.isAfter(endDate);
    }

    private CalendarEventResponse policyEvent(CalendarEventType eventType, Policy policy, LocalDate eventDate) {
        Long sourceId = policy.getId().longValue();
        return new CalendarEventResponse(
                eventType + ":" + sourceId + ":" + eventDate,
                eventType,
                eventDate,
                policy.getTitle(),
                null,
                sourceId,
                false
        );
    }

    private Comparator<CalendarEventResponse> eventComparator() {
        return Comparator
                .comparing(CalendarEventResponse::eventDate)
                .thenComparingInt(event -> eventPriority(event.eventType()))
                .thenComparing(CalendarEventResponse::title);
    }

    private int eventPriority(CalendarEventType eventType) {
        return switch (eventType) {
            case USER_SCHEDULE -> 0;
            case FIXED_EXPENSE_DUE -> 1;
            case POLICY_DEADLINE -> 2;
            case POLICY_SINGLE_DAY -> 3;
            case POLICY_START -> 4;
        };
    }
}
