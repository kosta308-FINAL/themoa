import { useState } from "react";
import DashboardIcon from "./DashboardIcon";
import "./CalendarPopover.css";

const WEEKDAYS = ["일", "월", "화", "수", "목", "금", "토"];

const pad = (n) => String(n).padStart(2, "0");
const toIso = (year, month, day) => `${year}-${pad(month)}-${pad(day)}`;

const buildCalendarDays = (year, month) => {
  const startWeekday = new Date(year, month - 1, 1).getDay();
  const daysInMonth = new Date(year, month, 0).getDate();
  const days = Array(startWeekday).fill(null);
  for (let day = 1; day <= daysInMonth; day += 1) days.push(day);
  return days;
};

/**
 * 트리거 요소의 부모(가장 가까운 position: relative 조상)를 기준으로
 * 펼쳐지는 달력 팝오버. 사용하는 쪽에서 트리거를 감싼 컨테이너에
 * position: relative를 지정해야 위치가 맞습니다.
 * placement="top"이면 트리거 위쪽으로, 기본값("bottom")이면 아래쪽으로 열립니다.
 */
function CalendarPopover({
  value,
  max,
  onSelect,
  onClose,
  title = "날짜 선택",
  placement = "bottom",
}) {
  const [selYear, selMonth] = (value || max).split("-").map(Number);
  const [maxYear, maxMonth] = max.split("-").map(Number);
  const [viewYear, setViewYear] = useState(selYear);
  const [viewMonth, setViewMonth] = useState(selMonth);

  const days = buildCalendarDays(viewYear, viewMonth);
  const yearOptions = Array.from({ length: 6 }, (_, i) => maxYear - 5 + i);
  const isAtMaxMonth = viewYear === maxYear && viewMonth === maxMonth;

  const changeMonth = (delta) => {
    let nextMonth = viewMonth + delta;
    let nextYear = viewYear;
    if (nextMonth < 1) {
      nextMonth = 12;
      nextYear -= 1;
    } else if (nextMonth > 12) {
      nextMonth = 1;
      nextYear += 1;
    }
    setViewYear(nextYear);
    setViewMonth(nextMonth);
  };

  return (
    <>
      <div
        className="calendar-popover-backdrop"
        role="presentation"
        onMouseDown={onClose}
      />
      <section
        className={`calendar-popover${placement === "top" ? " placement-top" : ""}`}
        role="dialog"
        aria-modal="true"
        aria-labelledby="calendar-popover-title"
        onMouseDown={(event) => event.stopPropagation()}
      >
        <div className="calendar-popover-head">
          <h2 id="calendar-popover-title">{title}</h2>
          <button
            type="button"
            className="calendar-popover-close"
            onClick={onClose}
            aria-label="닫기"
          >
            <DashboardIcon name="x" />
          </button>
        </div>
        <div className="calendar-popover-body">
          <div className="calendar-popover-controls">
            <button
              type="button"
              className="calendar-popover-arrow"
              aria-label="이전 달"
              onClick={() => changeMonth(-1)}
            >
              <DashboardIcon name="chevron-left" size={16} />
            </button>
            <div className="calendar-popover-selects">
              <select
                value={viewYear}
                onChange={(event) => setViewYear(Number(event.target.value))}
                aria-label="연도 선택"
              >
                {yearOptions.map((year) => (
                  <option key={year} value={year}>
                    {year}년
                  </option>
                ))}
              </select>
              <select
                value={viewMonth}
                onChange={(event) => setViewMonth(Number(event.target.value))}
                aria-label="월 선택"
              >
                {Array.from({ length: 12 }, (_, i) => i + 1).map((month) => (
                  <option key={month} value={month}>
                    {month}월
                  </option>
                ))}
              </select>
            </div>
            <button
              type="button"
              className="calendar-popover-arrow"
              aria-label="다음 달"
              disabled={isAtMaxMonth}
              onClick={() => changeMonth(1)}
            >
              <DashboardIcon name="chevron-right" size={16} />
            </button>
          </div>
          <div className="calendar-popover-weekdays">
            {WEEKDAYS.map((weekday) => (
              <span key={weekday}>{weekday}</span>
            ))}
          </div>
          <div className="calendar-popover-grid">
            {days.map((day, index) => {
              if (day === null)
                return <span key={`empty-${index}`} aria-hidden="true" />;
              const iso = toIso(viewYear, viewMonth, day);
              return (
                <button
                  type="button"
                  key={iso}
                  className={`calendar-popover-day${iso === value ? " selected" : ""}${iso === max ? " today" : ""}`}
                  disabled={iso > max}
                  onClick={() => onSelect(iso)}
                >
                  {day}
                </button>
              );
            })}
          </div>
        </div>
      </section>
    </>
  );
}

export default CalendarPopover;
