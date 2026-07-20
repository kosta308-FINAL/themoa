package com.weaone.themoa.domain.member.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDateTime;

/**
 * 시급제(HOURLY) 회원의 요일별 반복 근무시간(알바 소득 확장). 급여주기 생성 시
 * {@code WorkScheduleSalaryCalculator}가 이 스케줄을 그 주기 실제 날짜에 대입해 예상 소득을 산출한다.
 * 회원당 요일마다 1행만 허용하며, 설정 변경 시에는 개별 UPDATE 대신 전체 삭제 후 재생성한다.
 */
@Entity
@Table(name = "member_work_schedule",
        uniqueConstraints = @UniqueConstraint(name = "uk_work_schedule_member_day",
                columnNames = {"member_id", "day_of_week"}))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MemberWorkSchedule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @Enumerated(EnumType.STRING)
    @Column(name = "day_of_week", nullable = false, length = 10)
    private DayOfWeek dayOfWeek;

    @Column(nullable = false, precision = 4, scale = 2)
    private BigDecimal hours;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    private MemberWorkSchedule(Member member, DayOfWeek dayOfWeek, BigDecimal hours, LocalDateTime createdAt) {
        this.member = member;
        this.dayOfWeek = dayOfWeek;
        this.hours = hours;
        this.createdAt = createdAt;
    }

    public static MemberWorkSchedule create(Member member, DayOfWeek dayOfWeek, BigDecimal hours,
                                             LocalDateTime createdAt) {
        return new MemberWorkSchedule(member, dayOfWeek, hours, createdAt);
    }
}
