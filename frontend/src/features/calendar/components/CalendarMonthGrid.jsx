import { formatDateKey, isSameDate } from "../calendarDateUtils";

const DAY_LABELS = ["월", "화", "수", "목", "금", "토", "일"];

const EVENT_LABELS = {
  FIXED_EXPENSE_DUE: "출금 예정",
  POLICY_START: "신청 시작",
  POLICY_DEADLINE: "신청 마감",
  POLICY_SINGLE_DAY: "신청일",
  USER_SCHEDULE: "내 일정",
};

const typeClass = (eventType) => `calendar-event-${eventType.toLowerCase().replaceAll("_", "-")}`;

function CalendarMonthGrid({
  monthDate,
  today,
  monthDays,
  eventsByDate,
  selectedDateKey,
  loading,
  onSelectDate,
}) {
  return (
    <section className="calendar-month-card" aria-label="월간 캘린더">
      <div className="calendar-weekday-row">
        {DAY_LABELS.map((label) => (
          <span key={label}>{label}</span>
        ))}
      </div>
      <div className="calendar-month-grid">
        {monthDays.map((date) => {
          const dateKey = formatDateKey(date);
          const events = eventsByDate.get(dateKey) || [];
          const visibleEvents = events.slice(0, 3);
          const hiddenCount = events.length - visibleEvents.length;
          const isCurrentMonth = date.getMonth() === monthDate.getMonth();
          const selected = dateKey === selectedDateKey;
          const todayDate = isSameDate(date, today);
          const className = [
            "calendar-day-cell",
            isCurrentMonth ? "" : "is-outside-month",
            selected ? "is-selected" : "",
            todayDate ? "is-today" : "",
          ]
            .filter(Boolean)
            .join(" ");

          return (
            <button
              key={dateKey}
              type="button"
              className={className}
              onClick={() => onSelectDate(dateKey)}
              aria-label={`${date.getFullYear()}년 ${date.getMonth() + 1}월 ${date.getDate()}일 선택`}
            >
              <span className="calendar-day-number">{date.getDate()}</span>
              <div className="calendar-day-events">
                {loading && events.length === 0 ? (
                  <span className="calendar-event-skeleton" />
                ) : (
                  visibleEvents.map((event) => (
                    <span key={event.eventKey} className={`calendar-event-pill ${typeClass(event.eventType)}`}>
                      {EVENT_LABELS[event.eventType]}
                    </span>
                  ))
                )}
                {hiddenCount > 0 && <span className="calendar-event-more">+{hiddenCount}개</span>}
              </div>
            </button>
          );
        })}
      </div>
    </section>
  );
}

export default CalendarMonthGrid;
