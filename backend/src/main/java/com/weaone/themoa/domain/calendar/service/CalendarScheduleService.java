package com.weaone.themoa.domain.calendar.service;

import com.weaone.themoa.common.exception.BusinessException;
import com.weaone.themoa.common.exception.ErrorCode;
import com.weaone.themoa.domain.calendar.dto.request.CalendarScheduleCreateRequest;
import com.weaone.themoa.domain.calendar.dto.request.CalendarScheduleUpdateRequest;
import com.weaone.themoa.domain.calendar.dto.response.CalendarScheduleResponse;
import com.weaone.themoa.domain.calendar.entity.CalendarSchedule;
import com.weaone.themoa.domain.calendar.repository.CalendarScheduleRepository;
import com.weaone.themoa.domain.member.entity.Member;
import com.weaone.themoa.domain.member.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;

@Service
@RequiredArgsConstructor
public class CalendarScheduleService {
    private static final int MAX_TITLE_CODE_POINTS = 20;
    private static final ZoneId SEOUL_ZONE = ZoneId.of("Asia/Seoul");

    private final CalendarScheduleRepository scheduleRepository;
    private final MemberRepository memberRepository;

    @Transactional
    public CalendarScheduleResponse create(Long memberId, CalendarScheduleCreateRequest request) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));
        String title = normalizeTitle(request.title());
        CalendarSchedule schedule = CalendarSchedule.create(member, title, request.scheduleDate(), now());
        return CalendarScheduleResponse.from(scheduleRepository.save(schedule));
    }

    @Transactional
    public CalendarScheduleResponse update(Long memberId, Long scheduleId, CalendarScheduleUpdateRequest request) {
        CalendarSchedule schedule = scheduleRepository.findByIdAndMember_Id(scheduleId, memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.CALENDAR_SCHEDULE_NOT_FOUND));
        String title = normalizeTitle(request.title());
        schedule.update(title, request.scheduleDate(), now());
        return CalendarScheduleResponse.from(schedule);
    }

    @Transactional
    public void delete(Long memberId, Long scheduleId) {
        CalendarSchedule schedule = scheduleRepository.findByIdAndMember_Id(scheduleId, memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.CALENDAR_SCHEDULE_NOT_FOUND));
        scheduleRepository.delete(schedule);
    }

    private String normalizeTitle(String title) {
        if (title == null) {
            throw new BusinessException(ErrorCode.CALENDAR_SCHEDULE_TITLE_INVALID);
        }
        String normalizedTitle = title.trim();
        int titleLength = normalizedTitle.codePointCount(0, normalizedTitle.length());
        if (normalizedTitle.isBlank()
                || titleLength > MAX_TITLE_CODE_POINTS
                || containsControlCharacter(normalizedTitle)) {
            throw new BusinessException(ErrorCode.CALENDAR_SCHEDULE_TITLE_INVALID);
        }
        return normalizedTitle;
    }

    private boolean containsControlCharacter(String value) {
        return value.codePoints().anyMatch(Character::isISOControl);
    }

    private LocalDateTime now() {
        return LocalDateTime.now(SEOUL_ZONE);
    }
}
