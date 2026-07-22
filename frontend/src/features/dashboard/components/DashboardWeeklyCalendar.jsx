import { useMemo } from "react";

const DAY_LABELS = ["월", "화", "수", "목", "금", "토", "일"];
const FULL_DAY_LABELS = [
  "월요일",
  "화요일",
  "수요일",
  "목요일",
  "금요일",
  "토요일",
  "일요일",
];

const padDatePart = (value) => String(value).padStart(2, "0");

const getDateKey = (date) =>
  [
    date.getFullYear(),
    padDatePart(date.getMonth() + 1),
    padDatePart(date.getDate()),
  ].join("-");

const isSameDate = (left, right) =>
  left.getFullYear() === right.getFullYear() &&
  left.getMonth() === right.getMonth() &&
  left.getDate() === right.getDate();

const getMonday = (date) => {
  const monday = new Date(date);
  const day = monday.getDay();
  const mondayOffset = day === 0 ? -6 : 1 - day;
  monday.setDate(monday.getDate() + mondayOffset);
  monday.setHours(0, 0, 0, 0);
  return monday;
};

const formatShortDate = (date) => `${date.getMonth() + 1}.${date.getDate()}`;

const getAriaLabel = (date, dayIndex, today) => {
  const todayText = isSameDate(date, today) ? ", 오늘" : "";

  return `${date.getFullYear()}년 ${date.getMonth() + 1}월 ${date.getDate()}일 ${FULL_DAY_LABELS[dayIndex]}${todayText}`;
};

function DashboardWeeklyCalendar() {
  const today = useMemo(() => new Date(), []);
  const weekDays = useMemo(() => {
    const monday = getMonday(today);

    return DAY_LABELS.map((dayLabel, index) => {
      const date = new Date(monday);
      date.setDate(monday.getDate() + index);
      const isToday = isSameDate(date, today);
      const isWeekend = index >= 5;
      const classNames = [
        "dash-weekly-calendar-day",
        isToday ? "is-today" : "",
        isWeekend ? "is-weekend" : "",
      ]
        .filter(Boolean)
        .join(" ");

      return {
        date,
        dateKey: getDateKey(date),
        dayLabel,
        dayNumber: date.getDate(),
        ariaLabel: getAriaLabel(date, index, today),
        className: classNames,
      };
    });
  }, [today]);

  const rangeText = `${formatShortDate(weekDays[0].date)} - ${formatShortDate(
    weekDays[weekDays.length - 1].date,
  )}`;

  return (
    <section className="dash-weekly-calendar" aria-label="이번 주 일정">
      <div className="dash-weekly-calendar-header">
        <div>
          <span className="dash-weekly-calendar-eyebrow">주간 캘린더</span>
          <h3>이번 주 일정</h3>
        </div>
        <span className="dash-weekly-calendar-range">{rangeText}</span>
      </div>

      <div className="dash-weekly-calendar-grid">
        {weekDays.map((day) => (
          <time
            key={day.dateKey}
            dateTime={day.dateKey}
            className={day.className}
            aria-label={day.ariaLabel}
          >
            <span>{day.dayLabel}</span>
            <strong>{day.dayNumber}</strong>
          </time>
        ))}
      </div>

      <div className="dash-weekly-calendar-empty">
        <strong>일정 연동 준비 중</strong>
        <span>
          정책 신청일, 고정지출 예정일, 급여일은 다음 단계에서 연결됩니다.
        </span>
      </div>
    </section>
  );
}

export default DashboardWeeklyCalendar;
