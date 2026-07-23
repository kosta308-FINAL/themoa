package com.weaone.themoa.domain.calendar.service;

import com.weaone.themoa.common.exception.BusinessException;
import com.weaone.themoa.common.exception.ErrorCode;
import com.weaone.themoa.domain.calendar.dto.response.CalendarEventListResponse;
import com.weaone.themoa.domain.calendar.dto.response.CalendarEventResponse;
import com.weaone.themoa.domain.calendar.entity.CalendarEventType;
import com.weaone.themoa.domain.calendar.entity.CalendarSchedule;
import com.weaone.themoa.domain.calendar.repository.CalendarScheduleRepository;
import com.weaone.themoa.domain.category.entity.Category;
import com.weaone.themoa.domain.fixedexpense.entity.FixedExpense;
import com.weaone.themoa.domain.fixedexpense.entity.FixedExpensePaymentMethod;
import com.weaone.themoa.domain.fixedexpense.entity.FixedExpenseStatus;
import com.weaone.themoa.domain.fixedexpense.repository.FixedExpenseRepository;
import com.weaone.themoa.domain.member.entity.Gender;
import com.weaone.themoa.domain.member.entity.Member;
import com.weaone.themoa.domain.policy.bookmark.entity.PolicyBookmark;
import com.weaone.themoa.domain.policy.bookmark.repository.PolicyBookmarkRepository;
import com.weaone.themoa.domain.policy.policy.entity.Policy;
import com.weaone.themoa.domain.policy.policy.entity.PolicyCategory;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class CalendarQueryServiceTest {
    private final CalendarScheduleRepository scheduleRepository = mock(CalendarScheduleRepository.class);
    private final FixedExpenseRepository fixedExpenseRepository = mock(FixedExpenseRepository.class);
    private final PolicyBookmarkRepository policyBookmarkRepository = mock(PolicyBookmarkRepository.class);
    private final CalendarQueryService service =
            new CalendarQueryService(scheduleRepository, fixedExpenseRepository, policyBookmarkRepository);

    @Test
    void dateRangeAcceptsSameDayAndSixtyTwoDays() {
        LocalDate startDate = LocalDate.of(2026, 7, 1);
        LocalDate endDate = LocalDate.of(2026, 8, 31);
        givenEmpty(startDate, endDate);

        CalendarEventListResponse response = service.getEvents(7L, startDate, endDate);

        assertThat(response.startDate()).isEqualTo(startDate);
        assertThat(response.endDate()).isEqualTo(endDate);
    }

    @Test
    void dateRangeRejectsReversedRange() {
        assertDateRangeInvalid(() -> service.getEvents(
                7L,
                LocalDate.of(2026, 8, 1),
                LocalDate.of(2026, 7, 31)
        ));
    }

    @Test
    void dateRangeRejectsSixtyThreeDays() {
        assertDateRangeInvalid(() -> service.getEvents(
                7L,
                LocalDate.of(2026, 7, 1),
                LocalDate.of(2026, 9, 1)
        ));
    }

    @Test
    void fixedExpenseEventsUseActiveWithExpectedPayDayAndEndOfMonthCorrection() {
        LocalDate startDate = LocalDate.of(2026, 2, 1);
        LocalDate endDate = LocalDate.of(2026, 3, 31);
        FixedExpense netflix = fixedExpense(12L, "넷플릭스", (short) 31, "17000");
        given(scheduleRepository.findByMember_IdAndScheduleDateBetweenOrderByScheduleDateAscIdAsc(7L, startDate, endDate))
                .willReturn(List.of());
        given(fixedExpenseRepository.findByMember_IdAndStatusAndExpectedPayDayIsNotNull(7L, FixedExpenseStatus.ACTIVE))
                .willReturn(List.of(netflix));
        given(policyBookmarkRepository.findCalendarTargets(7L, startDate, endDate)).willReturn(List.of());

        CalendarEventListResponse response = service.getEvents(7L, startDate, endDate);

        assertThat(response.items()).extracting(CalendarEventResponse::eventType)
                .containsExactly(CalendarEventType.FIXED_EXPENSE_DUE, CalendarEventType.FIXED_EXPENSE_DUE);
        assertThat(response.items()).extracting(CalendarEventResponse::eventDate)
                .containsExactly(LocalDate.of(2026, 2, 28), LocalDate.of(2026, 3, 31));
        assertThat(response.items().get(0).amount()).isEqualByComparingTo("17000");
        verify(fixedExpenseRepository)
                .findByMember_IdAndStatusAndExpectedPayDayIsNotNull(7L, FixedExpenseStatus.ACTIVE);
    }

    @Test
    void fixedExpenseHandlesLeapYearFebruary() {
        LocalDate startDate = LocalDate.of(2028, 2, 1);
        LocalDate endDate = LocalDate.of(2028, 2, 29);
        given(scheduleRepository.findByMember_IdAndScheduleDateBetweenOrderByScheduleDateAscIdAsc(7L, startDate, endDate))
                .willReturn(List.of());
        given(fixedExpenseRepository.findByMember_IdAndStatusAndExpectedPayDayIsNotNull(7L, FixedExpenseStatus.ACTIVE))
                .willReturn(List.of(fixedExpense(12L, "보험료", (short) 31, "30000")));
        given(policyBookmarkRepository.findCalendarTargets(7L, startDate, endDate)).willReturn(List.of());

        CalendarEventListResponse response = service.getEvents(7L, startDate, endDate);

        assertThat(response.items()).extracting(CalendarEventResponse::eventDate)
                .containsExactly(LocalDate.of(2028, 2, 29));
    }

    @Test
    void policyEventsFollowDateRules() {
        LocalDate startDate = LocalDate.of(2026, 8, 1);
        LocalDate endDate = LocalDate.of(2026, 8, 31);
        Policy startOnly = policy(10, "시작 정책", LocalDate.of(2026, 8, 2), null, false);
        Policy dueOnly = policy(11, "마감 정책", null, LocalDate.of(2026, 8, 3), false);
        Policy both = policy(12, "양일 정책", LocalDate.of(2026, 8, 4), LocalDate.of(2026, 8, 9), false);
        Policy single = policy(13, "당일 정책", LocalDate.of(2026, 8, 5), LocalDate.of(2026, 8, 5), false);
        Policy alwaysOpen = policy(14, "상시 정책", LocalDate.of(2026, 8, 6), LocalDate.of(2026, 8, 7), true);
        given(scheduleRepository.findByMember_IdAndScheduleDateBetweenOrderByScheduleDateAscIdAsc(7L, startDate, endDate))
                .willReturn(List.of());
        given(fixedExpenseRepository.findByMember_IdAndStatusAndExpectedPayDayIsNotNull(7L, FixedExpenseStatus.ACTIVE))
                .willReturn(List.of());
        given(policyBookmarkRepository.findCalendarTargets(7L, startDate, endDate))
                .willReturn(List.of(
                        bookmark(startOnly),
                        bookmark(dueOnly),
                        bookmark(both),
                        bookmark(single),
                        bookmark(alwaysOpen)
                ));

        CalendarEventListResponse response = service.getEvents(7L, startDate, endDate);

        assertThat(response.items()).extracting(CalendarEventResponse::eventType)
                .containsExactly(
                        CalendarEventType.POLICY_START,
                        CalendarEventType.POLICY_DEADLINE,
                        CalendarEventType.POLICY_START,
                        CalendarEventType.POLICY_SINGLE_DAY,
                        CalendarEventType.POLICY_DEADLINE
                );
        assertThat(response.items()).extracting(CalendarEventResponse::sourceId)
                .containsExactly(10L, 11L, 12L, 13L, 12L);
    }

    @Test
    void eventsAreSortedByDatePriorityAndTitle() {
        LocalDate startDate = LocalDate.of(2026, 8, 5);
        LocalDate endDate = LocalDate.of(2026, 8, 5);
        Member member = member(7L);
        given(scheduleRepository.findByMember_IdAndScheduleDateBetweenOrderByScheduleDateAscIdAsc(7L, startDate, endDate))
                .willReturn(List.of(schedule(3L, member, "지원서 제출", startDate)));
        given(fixedExpenseRepository.findByMember_IdAndStatusAndExpectedPayDayIsNotNull(7L, FixedExpenseStatus.ACTIVE))
                .willReturn(List.of(fixedExpense(12L, "넷플릭스", (short) 5, "17000")));
        given(policyBookmarkRepository.findCalendarTargets(7L, startDate, endDate))
                .willReturn(List.of(bookmark(policy(13, "정책", startDate, startDate, false))));

        CalendarEventListResponse response = service.getEvents(7L, startDate, endDate);

        assertThat(response.items()).extracting(CalendarEventResponse::eventType)
                .containsExactly(
                        CalendarEventType.USER_SCHEDULE,
                        CalendarEventType.FIXED_EXPENSE_DUE,
                        CalendarEventType.POLICY_SINGLE_DAY
                );
    }

    private void givenEmpty(LocalDate startDate, LocalDate endDate) {
        given(scheduleRepository.findByMember_IdAndScheduleDateBetweenOrderByScheduleDateAscIdAsc(7L, startDate, endDate))
                .willReturn(List.of());
        given(fixedExpenseRepository.findByMember_IdAndStatusAndExpectedPayDayIsNotNull(7L, FixedExpenseStatus.ACTIVE))
                .willReturn(List.of());
        given(policyBookmarkRepository.findCalendarTargets(7L, startDate, endDate)).willReturn(List.of());
    }

    private void assertDateRangeInvalid(Runnable runnable) {
        assertThatThrownBy(runnable::run)
                .isInstanceOfSatisfying(BusinessException.class,
                        ex -> assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.CALENDAR_DATE_RANGE_INVALID));
    }

    private Member member(Long id) {
        Member member = Member.signUp(
                "user" + id + "@example.com",
                "password",
                "회원",
                Gender.MALE,
                LocalDate.of(1998, 1, 1),
                LocalDateTime.of(2026, 7, 21, 0, 0)
        );
        ReflectionTestUtils.setField(member, "id", id);
        return member;
    }

    private CalendarSchedule schedule(Long id, Member member, String title, LocalDate scheduleDate) {
        CalendarSchedule schedule = CalendarSchedule.create(
                member,
                title,
                scheduleDate,
                LocalDateTime.of(2026, 7, 23, 10, 0)
        );
        ReflectionTestUtils.setField(schedule, "id", id);
        return schedule;
    }

    private FixedExpense fixedExpense(Long id, String name, short expectedPayDay, String expectedAmountKrw) {
        FixedExpense fixedExpense = FixedExpense.registerDirect(
                member(7L),
                name,
                mock(Category.class),
                null,
                FixedExpensePaymentMethod.TRANSFER,
                expectedPayDay,
                new BigDecimal(expectedAmountKrw),
                "KRW",
                new BigDecimal(expectedAmountKrw),
                null,
                null
        );
        ReflectionTestUtils.setField(fixedExpense, "id", id);
        return fixedExpense;
    }

    private Policy policy(Integer id, String title, LocalDate startDate, LocalDate dueDate, boolean alwaysOpen) {
        Policy policy = new Policy("YC-" + id);
        ReflectionTestUtils.setField(policy, "id", id);
        policy.updateBasic(
                title,
                "경기도",
                PolicyCategory.일자리,
                "요약",
                "https://example.com",
                startDate,
                dueDate,
                alwaysOpen,
                true,
                "신청중"
        );
        return policy;
    }

    private PolicyBookmark bookmark(Policy policy) {
        return PolicyBookmark.interest(member(7L), policy);
    }
}
