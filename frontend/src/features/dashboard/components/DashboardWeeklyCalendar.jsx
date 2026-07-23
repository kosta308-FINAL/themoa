import { useMemo, useState } from "react";
import { Link } from "react-router-dom";

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

const EVENT_LABELS = {
  FIXED_EXPENSE_DUE: "출금 예정",
  POLICY_START: "신청 시작",
  POLICY_DEADLINE: "신청 마감",
  POLICY_SINGLE_DAY: "신청일",
  USER_SCHEDULE: "내 일정",
};

const getAriaLabel = (date, dayIndex, today) => {
  const todayText = isSameDate(date, today) ? ", 오늘" : "";

  return `${date.getFullYear()}년 ${date.getMonth() + 1}월 ${date.getDate()}일 ${FULL_DAY_LABELS[dayIndex]}${todayText}`;
};

const typeClass = (eventType) => `dash-weekly-event-${eventType.toLowerCase().replaceAll("_", "-")}`;

function DashboardWeeklyCalendar({ events = [], loading, error }) {
  const today = useMemo(() => new Date(), []);
  const [selectedDateKey, setSelectedDateKey] = useState(() => getDateKey(today));
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
  const eventsByDate = useMemo(() => {
    const groups = new Map();
    events.forEach((event) => {
      const items = groups.get(event.eventDate) || [];
      groups.set(event.eventDate, [...items, event]);
    });
    return groups;
  }, [events]);
  const selectedEvents = eventsByDate.get(selectedDateKey) || [];
  const visibleSelectedEvents = selectedEvents.slice(0, 3);
  const hiddenSelectedCount = selectedEvents.length - visibleSelectedEvents.length;

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
        {weekDays.map((day) => {
          const dayEvents = eventsByDate.get(day.dateKey) || [];
          return (
            <button
              key={day.dateKey}
              type="button"
              className={`${day.className}${selectedDateKey === day.dateKey ? " is-selected" : ""}`}
              onClick={() => setSelectedDateKey(day.dateKey)}
              aria-label={day.ariaLabel}
            >
              <time dateTime={day.dateKey}>
                <span>{day.dayLabel}</span>
                <strong>{day.dayNumber}</strong>
              </time>
              {dayEvents.length > 0 && (
                <span className="dash-weekly-event-dots" aria-label={`${dayEvents.length}개 일정`}>
                  {dayEvents.slice(0, 3).map((event) => (
                    <span key={event.eventKey} className={typeClass(event.eventType)} />
                  ))}
                </span>
              )}
            </button>
          );
        })}
      </div>

      <div className="dash-weekly-calendar-summary">
        {loading && <div className="dash-weekly-calendar-empty">이번 주 일정을 불러오는 중입니다.</div>}
        {!loading && error && <div className="dash-weekly-calendar-empty is-error">{error}</div>}
        {!loading && !error && selectedEvents.length === 0 && (
          <div className="dash-weekly-calendar-empty">등록된 일정이 없어요.</div>
        )}
        {!loading && !error && selectedEvents.length > 0 && (
          <ul className="dash-weekly-event-list">
            {visibleSelectedEvents.map((event) => (
              <li key={event.eventKey}>
                <span className={typeClass(event.eventType)}>{EVENT_LABELS[event.eventType]}</span>
                <strong>{event.title}</strong>
              </li>
            ))}
            {hiddenSelectedCount > 0 && <li className="dash-weekly-event-more">외 {hiddenSelectedCount}건</li>}
          </ul>
        )}
        <Link className="dash-weekly-calendar-link" to="/dashboard/calendar">
          전체 캘린더
        </Link>
      </div>
    </section>
  );
}

export default DashboardWeeklyCalendar;
