import { useMemo, useState } from "react";
import DashboardIcon from "../../components/common/DashboardIcon";
import { formatDateKey, parseDateKey } from "./calendarDateUtils";
import CalendarDayPanel from "./components/CalendarDayPanel";
import CalendarMonthGrid from "./components/CalendarMonthGrid";
import { useCalendar } from "./hooks/useCalendar";
import "./CalendarPage.css";

const FILTERS = [
  { key: "ALL", label: "전체" },
  { key: "FIXED_EXPENSE", label: "고정지출" },
  { key: "POLICY", label: "정책" },
  { key: "USER_SCHEDULE", label: "내 일정" },
];

const getCalendarStart = (date) => {
  const firstDay = new Date(date.getFullYear(), date.getMonth(), 1);
  const day = firstDay.getDay();
  const mondayOffset = day === 0 ? -6 : 1 - day;
  firstDay.setDate(firstDay.getDate() + mondayOffset);
  return firstDay;
};

const buildMonthDays = (monthDate) => {
  const start = getCalendarStart(monthDate);
  return Array.from({ length: 42 }, (_, index) => {
    const date = new Date(start);
    date.setDate(start.getDate() + index);
    return date;
  });
};

const isPolicyEvent = (eventType) => eventType.startsWith("POLICY_");

const matchesFilter = (event, filter) => {
  if (filter === "ALL") return true;
  if (filter === "FIXED_EXPENSE") return event.eventType === "FIXED_EXPENSE_DUE";
  if (filter === "POLICY") return isPolicyEvent(event.eventType);
  return event.eventType === "USER_SCHEDULE";
};

function CalendarPage() {
  const today = useMemo(() => new Date(), []);
  const [monthDate, setMonthDate] = useState(() => new Date(today.getFullYear(), today.getMonth(), 1));
  const [selectedDateKey, setSelectedDateKey] = useState(() => formatDateKey(today));
  const [filter, setFilter] = useState("ALL");
  const monthDays = useMemo(() => buildMonthDays(monthDate), [monthDate]);
  const range = useMemo(
    () => ({
      startDate: formatDateKey(monthDays[0]),
      endDate: formatDateKey(monthDays[monthDays.length - 1]),
    }),
    [monthDays],
  );
  const calendar = useCalendar(range);
  const filteredEvents = useMemo(
    () => calendar.events.filter((event) => matchesFilter(event, filter)),
    [calendar.events, filter],
  );
  const eventsByDate = useMemo(() => {
    const groups = new Map();
    filteredEvents.forEach((event) => {
      const items = groups.get(event.eventDate) || [];
      groups.set(event.eventDate, [...items, event]);
    });
    return groups;
  }, [filteredEvents]);
  const selectedDate = useMemo(() => parseDateKey(selectedDateKey), [selectedDateKey]);
  const selectedEvents = eventsByDate.get(selectedDateKey) || [];
  const monthLabel = `${monthDate.getFullYear()}년 ${monthDate.getMonth() + 1}월`;

  const moveMonth = (offset) => {
    setMonthDate((current) => new Date(current.getFullYear(), current.getMonth() + offset, 1));
  };

  const moveToday = () => {
    const nextToday = new Date();
    setMonthDate(new Date(nextToday.getFullYear(), nextToday.getMonth(), 1));
    setSelectedDateKey(formatDateKey(nextToday));
  };

  return (
    <main className="dash-main calendar-page">
      <div className="dash-topbar calendar-topbar">
        <div>
          <h1>캘린더</h1>
          <p>고정지출 출금 예정일, 관심 정책 일정과 직접 등록한 일정을 확인하세요.</p>
        </div>
      </div>

      <section className="calendar-toolbar">
        <div className="calendar-month-controls">
          <button type="button" onClick={() => moveMonth(-1)} aria-label="이전 달">
            <DashboardIcon name="chevron-left" size={16} />
            이전 달
          </button>
          <strong>{monthLabel}</strong>
          <button type="button" onClick={moveToday}>
            오늘
          </button>
          <button type="button" onClick={() => moveMonth(1)} aria-label="다음 달">
            다음 달
            <DashboardIcon name="chevron-right" size={16} />
          </button>
        </div>
        <div className="calendar-filter-group" aria-label="캘린더 필터">
          {FILTERS.map((item) => (
            <button
              key={item.key}
              type="button"
              className={filter === item.key ? "active" : ""}
              onClick={() => setFilter(item.key)}
            >
              {item.label}
            </button>
          ))}
        </div>
      </section>

      {calendar.error && (
        <div className="calendar-section-error">
          <span>{calendar.error}</span>
          <button type="button" onClick={calendar.reload}>
            다시 시도
          </button>
        </div>
      )}

      <section className="calendar-layout">
        <CalendarMonthGrid
          monthDate={monthDate}
          today={today}
          monthDays={monthDays}
          eventsByDate={eventsByDate}
          selectedDateKey={selectedDateKey}
          loading={calendar.isLoading}
          onSelectDate={setSelectedDateKey}
        />
        <CalendarDayPanel
          selectedDate={selectedDate}
          selectedDateKey={selectedDateKey}
          events={selectedEvents}
          isSaving={calendar.isSaving}
          mutationError={calendar.mutationError}
          onCreate={calendar.createSchedule}
          onUpdate={calendar.updateSchedule}
          onDelete={calendar.deleteSchedule}
        />
      </section>
    </main>
  );
}

export default CalendarPage;
