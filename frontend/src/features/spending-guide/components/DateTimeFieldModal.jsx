import { useEffect, useRef, useState } from "react";
import DashboardIcon from "../../../components/common/DashboardIcon";

const WEEKDAYS = ["일", "월", "화", "수", "목", "금", "토"];
const MERIDIEMS = ["오전", "오후"];

const pad = (n) => String(n).padStart(2, "0");

const parseValue = (value) => {
  const [datePart, timePart] = (value || "").split("T");
  const [year, month, day] = (datePart || "").split("-").map(Number);
  const [hour, minute] = (timePart || "00:00").split(":").map(Number);
  const now = new Date();
  return {
    year: year || now.getFullYear(),
    month: month || now.getMonth() + 1,
    day: day || now.getDate(),
    hour: hour || 0,
    minute: minute || 0,
  };
};

const toIso = (year, month, day) => `${year}-${pad(month)}-${pad(day)}`;

const toValue = ({ year, month, day, hour, minute }) =>
  `${toIso(year, month, day)}T${pad(hour)}:${pad(minute)}`;

const buildCalendarDays = (year, month) => {
  const startWeekday = new Date(year, month - 1, 1).getDay();
  const daysInMonth = new Date(year, month, 0).getDate();
  const days = Array(startWeekday).fill(null);
  for (let day = 1; day <= daysInMonth; day += 1) days.push(day);
  return days;
};

/**
 * datetime-local 네이티브 입력 대신 사용하는 커스텀 날짜/시간 선택 필드.
 * 트리거를 누르면 달력+시간 선택 모달이 뜨고, max를 넘는 값은 자동으로 clamp됩니다.
 */
function DateTimeFieldModal({ label, value, max, onChange, wide = false }) {
  const [isOpen, setIsOpen] = useState(false);
  const [viewMode, setViewMode] = useState("date");
  const [viewYear, setViewYear] = useState(() => parseValue(value).year);
  const [viewMonth, setViewMonth] = useState(() => parseValue(value).month);
  const hourListRef = useRef(null);
  const minuteListRef = useRef(null);

  const selected = parseValue(value);
  const maxParsed = parseValue(max);
  const maxDateIso = toIso(maxParsed.year, maxParsed.month, maxParsed.day);
  const selectedIso = toIso(selected.year, selected.month, selected.day);
  const hour12 = selected.hour % 12 === 0 ? 12 : selected.hour % 12;
  const meridiem = selected.hour < 12 ? "오전" : "오후";

  useEffect(() => {
    if (!isOpen) return;
    const frame = requestAnimationFrame(() => {
      hourListRef.current
        ?.querySelector(".selected")
        ?.scrollIntoView({ block: "center" });
      minuteListRef.current
        ?.querySelector(".selected")
        ?.scrollIntoView({ block: "center" });
    });
    return () => cancelAnimationFrame(frame);
  }, [isOpen]);

  const openPicker = () => {
    setViewYear(selected.year);
    setViewMonth(selected.month);
    setViewMode("date");
    setIsOpen(true);
  };
  const closePicker = () => setIsOpen(false);

  const days = buildCalendarDays(viewYear, viewMonth);
  const yearOptions = Array.from(
    { length: 12 },
    (_, i) => maxParsed.year - 8 + i,
  );

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

  const commit = (next) => {
    const nextValue = toValue(next);
    onChange(nextValue > max ? max : nextValue);
  };

  const pickDay = (day) =>
    commit({ ...selected, year: viewYear, month: viewMonth, day });
  const pickHour12 = (hour) => {
    const isPM = selected.hour >= 12;
    commit({ ...selected, hour: isPM ? (hour % 12) + 12 : hour % 12 });
  };
  const pickMinute = (minute) => commit({ ...selected, minute });
  const pickMeridiem = (label2) => {
    const base = selected.hour % 12;
    commit({ ...selected, hour: label2 === "오후" ? base + 12 : base });
  };

  const displayLabel = `${selected.year}-${pad(selected.month)}-${pad(
    selected.day,
  )} ${meridiem} ${pad(hour12)}:${pad(selected.minute)}`;

  return (
    <label className={`spending-select-field${wide ? " wide" : ""}`}>
      <span>{label}</span>
      <button
        type="button"
        className="spending-select-trigger"
        aria-expanded={isOpen}
        onClick={() => (isOpen ? closePicker() : openPicker())}
      >
        <span>{displayLabel}</span>
        <DashboardIcon name="calendar" size={16} />
      </button>
      {isOpen && (
        <>
          <div
            className="spending-datetime-backdrop"
            role="presentation"
            onMouseDown={closePicker}
          />
          <section
            className="spending-datetime-modal"
            role="dialog"
            aria-modal="true"
            aria-label={label}
            onMouseDown={(event) => event.stopPropagation()}
          >
            <div className="spending-datetime-head">
              <h3>사용일시 선택</h3>
              <button
                type="button"
                className="spending-modal-close"
                onClick={closePicker}
                aria-label="닫기"
              >
                <DashboardIcon name="x" />
              </button>
            </div>
            <div className="spending-datetime-body">
              <div className="spending-datetime-calendar">
                {viewMode === "date" ? (
                  <>
                    <div className="spending-datetime-cal-nav">
                      <button
                        type="button"
                        className="spending-datetime-title"
                        onClick={() => setViewMode("year")}
                      >
                        {viewYear}년 {viewMonth}월
                        <DashboardIcon name="chevron-down" size={14} />
                      </button>
                      <div>
                        <button
                          type="button"
                          aria-label="이전 달"
                          onClick={() => changeMonth(-1)}
                        >
                          <DashboardIcon name="chevron-left" size={16} />
                        </button>
                        <button
                          type="button"
                          aria-label="다음 달"
                          disabled={
                            viewYear === maxParsed.year &&
                            viewMonth === maxParsed.month
                          }
                          onClick={() => changeMonth(1)}
                        >
                          <DashboardIcon name="chevron-right" size={16} />
                        </button>
                      </div>
                    </div>
                    <div className="spending-datetime-weekdays">
                      {WEEKDAYS.map((weekday) => (
                        <span key={weekday}>{weekday}</span>
                      ))}
                    </div>
                    <div className="spending-datetime-grid">
                      {days.map((day, index) => {
                        if (day === null)
                          return (
                            <span key={`empty-${index}`} aria-hidden="true" />
                          );
                        const iso = toIso(viewYear, viewMonth, day);
                        return (
                          <button
                            type="button"
                            key={iso}
                            className={
                              iso === selectedIso
                                ? "selected"
                                : iso === maxDateIso
                                  ? "today"
                                  : ""
                            }
                            disabled={iso > maxDateIso}
                            onClick={() => pickDay(day)}
                          >
                            {day}
                          </button>
                        );
                      })}
                    </div>
                  </>
                ) : (
                  <div className="spending-datetime-year-grid">
                    {yearOptions.map((year) => (
                      <button
                        type="button"
                        key={year}
                        className={year === viewYear ? "selected" : ""}
                        disabled={year > maxParsed.year}
                        onClick={() => {
                          setViewYear(year);
                          if (year === maxParsed.year) {
                            setViewMonth((month) =>
                              Math.min(month, maxParsed.month),
                            );
                          }
                          setViewMode("date");
                        }}
                      >
                        {year}년
                      </button>
                    ))}
                  </div>
                )}
              </div>
              <div className="spending-datetime-time">
                <div className="spending-datetime-time-group">
                  <span className="spending-datetime-time-label">시</span>
                  <div className="spending-datetime-time-col" ref={hourListRef}>
                    {Array.from({ length: 12 }, (_, i) => i + 1).map((hour) => (
                      <button
                        type="button"
                        key={hour}
                        className={hour === hour12 ? "selected" : ""}
                        onClick={() => pickHour12(hour)}
                      >
                        {pad(hour)}
                      </button>
                    ))}
                  </div>
                </div>
                <div className="spending-datetime-time-group">
                  <span className="spending-datetime-time-label">분</span>
                  <div
                    className="spending-datetime-time-col"
                    ref={minuteListRef}
                  >
                    {Array.from({ length: 60 }, (_, i) => i).map((minute) => (
                      <button
                        type="button"
                        key={minute}
                        className={minute === selected.minute ? "selected" : ""}
                        onClick={() => pickMinute(minute)}
                      >
                        {pad(minute)}
                      </button>
                    ))}
                  </div>
                </div>
                <div className="spending-datetime-time-group">
                  <span
                    className="spending-datetime-time-label"
                    aria-hidden="true"
                  >
                    &nbsp;
                  </span>
                  <div className="spending-datetime-time-col meridiem">
                    {MERIDIEMS.map((label2) => (
                      <button
                        type="button"
                        key={label2}
                        className={label2 === meridiem ? "selected" : ""}
                        onClick={() => pickMeridiem(label2)}
                      >
                        {label2}
                      </button>
                    ))}
                  </div>
                </div>
              </div>
            </div>
            <div className="spending-datetime-actions">
              <button
                type="button"
                className="spending-primary"
                onClick={() => setIsOpen(false)}
              >
                확인
              </button>
            </div>
          </section>
        </>
      )}
    </label>
  );
}

export default DateTimeFieldModal;
