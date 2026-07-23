package com.weaone.themoa.domain.calendar.service;

import com.weaone.themoa.common.exception.BusinessException;
import com.weaone.themoa.common.exception.ErrorCode;
import com.weaone.themoa.domain.calendar.dto.request.CalendarScheduleCreateRequest;
import com.weaone.themoa.domain.calendar.dto.request.CalendarScheduleUpdateRequest;
import com.weaone.themoa.domain.calendar.dto.response.CalendarScheduleResponse;
import com.weaone.themoa.domain.calendar.entity.CalendarSchedule;
import com.weaone.themoa.domain.calendar.repository.CalendarScheduleRepository;
import com.weaone.themoa.domain.member.entity.Gender;
import com.weaone.themoa.domain.member.entity.Member;
import com.weaone.themoa.domain.member.repository.MemberRepository;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class CalendarScheduleServiceTest {
    private final CalendarScheduleRepository scheduleRepository = mock(CalendarScheduleRepository.class);
    private final MemberRepository memberRepository = mock(MemberRepository.class);
    private final CalendarScheduleService service = new CalendarScheduleService(scheduleRepository, memberRepository);

    @Test
    void createAcceptsOneCharacterTitle() {
        Member member = member(7L);
        given(memberRepository.findById(7L)).willReturn(Optional.of(member));
        given(scheduleRepository.save(any(CalendarSchedule.class))).willAnswer(invocation -> {
            CalendarSchedule schedule = invocation.getArgument(0);
            ReflectionTestUtils.setField(schedule, "id", 3L);
            return schedule;
        });

        CalendarScheduleResponse response = service.create(
                7L,
                new CalendarScheduleCreateRequest("가", LocalDate.of(2026, 8, 5))
        );

        assertThat(response.id()).isEqualTo(3L);
        assertThat(response.title()).isEqualTo("가");
        assertThat(response.scheduleDate()).isEqualTo(LocalDate.of(2026, 8, 5));
    }

    @Test
    void createAcceptsTwentyKoreanCharactersAndTrimsSpaces() {
        Member member = member(7L);
        String title = "가나다라마바사아자차카타파하가나다라마바";
        given(memberRepository.findById(7L)).willReturn(Optional.of(member));
        given(scheduleRepository.save(any(CalendarSchedule.class))).willAnswer(invocation -> invocation.getArgument(0));

        CalendarScheduleResponse response = service.create(
                7L,
                new CalendarScheduleCreateRequest("  " + title + "  ", LocalDate.of(2026, 8, 5))
        );

        assertThat(response.title()).isEqualTo(title);
    }

    @Test
    void createRejectsTwentyOneKoreanCharacters() {
        given(memberRepository.findById(7L)).willReturn(Optional.of(member(7L)));
        String title = "가나다라마바사아자차카타파하가나다라마바사";

        assertCalendarTitleInvalid(() -> service.create(
                7L,
                new CalendarScheduleCreateRequest(title, LocalDate.of(2026, 8, 5))
        ));
    }

    @Test
    void createRejectsBlankTitle() {
        given(memberRepository.findById(7L)).willReturn(Optional.of(member(7L)));

        assertCalendarTitleInvalid(() -> service.create(
                7L,
                new CalendarScheduleCreateRequest("   ", LocalDate.of(2026, 8, 5))
        ));
    }

    @Test
    void createRejectsControlCharacters() {
        given(memberRepository.findById(7L)).willReturn(Optional.of(member(7L)));

        assertCalendarTitleInvalid(() -> service.create(
                7L,
                new CalendarScheduleCreateRequest("지원서\n제출", LocalDate.of(2026, 8, 5))
        ));
    }

    @Test
    void createThrowsMemberNotFound() {
        given(memberRepository.findById(7L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> service.create(
                7L,
                new CalendarScheduleCreateRequest("지원서 제출", LocalDate.of(2026, 8, 5))
        )).isInstanceOfSatisfying(BusinessException.class,
                ex -> assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.MEMBER_NOT_FOUND));
    }

    @Test
    void updateOwnSchedule() {
        CalendarSchedule schedule = schedule(3L, member(7L), "지원서 제출", LocalDate.of(2026, 8, 5));
        given(scheduleRepository.findByIdAndMember_Id(3L, 7L)).willReturn(Optional.of(schedule));

        CalendarScheduleResponse response = service.update(
                7L,
                3L,
                new CalendarScheduleUpdateRequest("지원서 최종 제출", LocalDate.of(2026, 8, 6))
        );

        assertThat(response.title()).isEqualTo("지원서 최종 제출");
        assertThat(response.scheduleDate()).isEqualTo(LocalDate.of(2026, 8, 6));
    }

    @Test
    void updateOtherMemberScheduleAsNotFound() {
        given(scheduleRepository.findByIdAndMember_Id(3L, 7L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> service.update(
                7L,
                3L,
                new CalendarScheduleUpdateRequest("지원서 제출", LocalDate.of(2026, 8, 5))
        )).isInstanceOfSatisfying(BusinessException.class,
                ex -> assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.CALENDAR_SCHEDULE_NOT_FOUND));
    }

    @Test
    void deleteOwnSchedule() {
        CalendarSchedule schedule = schedule(3L, member(7L), "지원서 제출", LocalDate.of(2026, 8, 5));
        given(scheduleRepository.findByIdAndMember_Id(3L, 7L)).willReturn(Optional.of(schedule));

        service.delete(7L, 3L);

        verify(scheduleRepository).delete(schedule);
    }

    @Test
    void deleteOtherMemberScheduleAsNotFound() {
        given(scheduleRepository.findByIdAndMember_Id(3L, 7L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> service.delete(7L, 3L))
                .isInstanceOfSatisfying(BusinessException.class,
                        ex -> assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.CALENDAR_SCHEDULE_NOT_FOUND));
    }

    private void assertCalendarTitleInvalid(Runnable runnable) {
        assertThatThrownBy(runnable::run)
                .isInstanceOfSatisfying(BusinessException.class,
                        ex -> assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.CALENDAR_SCHEDULE_TITLE_INVALID));
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
}
