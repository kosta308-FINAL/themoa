package com.weaone.themoa.domain.calendar.entity;

import com.weaone.themoa.domain.member.entity.Member;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "calendar_schedule",
        indexes = {
                @Index(
                        name = "idx_calendar_schedule_member_date",
                        columnList = "member_id,schedule_date"
                )
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CalendarSchedule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @Column(nullable = false, length = 20)
    private String title;

    @Column(name = "schedule_date", nullable = false)
    private LocalDate scheduleDate;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public static CalendarSchedule create(Member member, String title, LocalDate scheduleDate, LocalDateTime now) {
        CalendarSchedule schedule = new CalendarSchedule();
        schedule.member = member;
        schedule.title = title;
        schedule.scheduleDate = scheduleDate;
        schedule.createdAt = now;
        schedule.updatedAt = now;
        return schedule;
    }

    public void update(String title, LocalDate scheduleDate, LocalDateTime now) {
        this.title = title;
        this.scheduleDate = scheduleDate;
        this.updatedAt = now;
    }
}
