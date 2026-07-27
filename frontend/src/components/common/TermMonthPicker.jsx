import { useEffect, useRef } from "react";
import DashboardIcon from "./DashboardIcon";
import "./TermMonthPicker.css";

/**
 * 가입기간(개월) 을 버튼으로 고르는 팝오버. 고르는 즉시 선택되고 닫힌다.
 * 트리거를 감싼 컨테이너에 position: relative가 필요하다(CalendarPopover와 동일한 규칙).
 */
function TermMonthPicker({ options, value, onSelect, onClose }) {
  const listRef = useRef(null);

  useEffect(() => {
    const frame = requestAnimationFrame(() => {
      listRef.current
        ?.querySelector(".selected")
        ?.scrollIntoView({ block: "center" });
    });
    return () => cancelAnimationFrame(frame);
  }, []);

  return (
    <>
      <div
        className="term-picker-backdrop"
        role="presentation"
        onMouseDown={onClose}
      />
      <section
        className="term-picker"
        role="dialog"
        aria-modal="true"
        aria-labelledby="term-picker-title"
        onMouseDown={(event) => event.stopPropagation()}
      >
        <div className="term-picker-head">
          <h2 id="term-picker-title">가입기간 선택</h2>
          <button
            type="button"
            className="term-picker-close"
            onClick={onClose}
            aria-label="닫기"
          >
            <DashboardIcon name="x" size={14} />
          </button>
        </div>
        <ul className="term-picker-list" ref={listRef}>
          {options.map((month) => (
            <li key={month}>
              <button
                type="button"
                className={Number(value) === Number(month) ? "selected" : ""}
                onClick={() => onSelect(month)}
              >
                {month}개월
              </button>
            </li>
          ))}
        </ul>
      </section>
    </>
  );
}

export default TermMonthPicker;
