package com.weaone.themoa.domain.calendar.repository;

import com.weaone.themoa.domain.calendar.entity.CalendarSchedule;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface CalendarScheduleRepository extends JpaRepository<CalendarSchedule, Long> {

    List<CalendarSchedule> findByMember_IdAndScheduleDateBetweenOrderByScheduleDateAscIdAsc(
            Long memberId,
            LocalDate startDate,
            LocalDate endDate
    );

    Optional<CalendarSchedule> findByIdAndMember_Id(Long scheduleId, Long memberId);
}
