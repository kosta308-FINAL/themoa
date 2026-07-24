import { useEffect, useRef } from "react";
import DashboardIcon from "./DashboardIcon";
import {
  HOUR_OPTIONS,
  parseDecimalHours,
} from "../../utils/workScheduleEstimate";
import "./HourPicker.css";

/**
 * 근무시간(시) 을 0~23 버튼으로 고르는 팝오버. 고르는 즉시 선택되고 닫힌다.
 * 트리거를 감싼 컨테이너에 position: relative가 필요하다(CalendarPopover와 동일한 규칙).
 */
function HourPicker({ value, onChange, onClose }) {
  const { hour: selectedHour } = parseDecimalHours(value);
  const hourListRef = useRef(null);

  useEffect(() => {
    const frame = requestAnimationFrame(() => {
      hourListRef.current
        ?.querySelector(".selected")
        ?.scrollIntoView({ block: "center" });
    });
    return () => cancelAnimationFrame(frame);
  }, []);

  const pickHour = (hour) => {
    onChange(hour);
    onClose();
  };

  return (
    <>
      <div
        className="hour-picker-backdrop"
        role="presentation"
        onMouseDown={onClose}
      />
      <section
        className="hour-picker"
        role="dialog"
        aria-modal="true"
        aria-labelledby="hour-picker-title"
        onMouseDown={(event) => event.stopPropagation()}
      >
        <div className="hour-picker-head">
          <h2 id="hour-picker-title">근무시간 선택</h2>
          <button
            type="button"
            className="hour-picker-close"
            onClick={onClose}
            aria-label="닫기"
          >
            <DashboardIcon name="x" size={14} />
          </button>
        </div>
        <div className="hour-picker-grid" ref={hourListRef}>
          {HOUR_OPTIONS.map((hour) => (
            <button
              type="button"
              key={hour}
              className={hour === selectedHour ? "selected" : ""}
              onClick={() => pickHour(hour)}
            >
              {hour}시
            </button>
          ))}
        </div>
      </section>
    </>
  );
}

export default HourPicker;
