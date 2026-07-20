package com.weaone.themoa.domain.budget.service;

import com.weaone.themoa.domain.member.entity.MemberWorkSchedule;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 시급제(HOURLY) 회원의 급여주기별 예상 소득 산출(알바 소득 확장). 요일별 반복 스케줄을 그 주기의
 * 실제 날짜 범위에 대입해 각 요일이 몇 번 들어있는지 계산한다 — 평균 근사가 아니라 달마다 실제
 * 등장 횟수(예: 토요일이 5번 있는 달)를 그대로 반영한다.
 */
@Component
public class WorkScheduleSalaryCalculator {

    public BigDecimal calculate(List<MemberWorkSchedule> schedules, BigDecimal hourlyWage,
                                 LocalDate cycleStartDate, LocalDate cycleEndDate) {
        if (schedules.isEmpty() || hourlyWage == null) {
            return BigDecimal.ZERO;
        }
        // UNIQUE(member_id, day_of_week)로 요일당 1행만 존재하므로 병합 함수가 필요 없다.
        Map<DayOfWeek, BigDecimal> hoursByDay = schedules.stream()
                .collect(Collectors.toMap(MemberWorkSchedule::getDayOfWeek, MemberWorkSchedule::getHours));

        BigDecimal totalHours = BigDecimal.ZERO;
        for (LocalDate date = cycleStartDate; !date.isAfter(cycleEndDate); date = date.plusDays(1)) {
            BigDecimal hours = hoursByDay.get(date.getDayOfWeek());
            if (hours != null) {
                totalHours = totalHours.add(hours);
            }
        }
        return totalHours.multiply(hourlyWage);
    }
}
