import { useEffect, useRef } from "react";
import DashboardIcon from "./DashboardIcon";
import "./OptionPicker.css";

/**
 * { value, label } 옵션 목록을 버튼으로 고르는 팝오버. 고르는 즉시 선택되고 닫힌다.
 * 트리거를 감싼 컨테이너에 position: relative가 필요하다(CalendarPopover와 동일한 규칙).
 */
function OptionPicker({ options, value, onSelect, onClose, title = "선택" }) {
  const listRef = useRef(null);

  useEffect(() => {
    const frame = requestAnimationFrame(() => {
      listRef.current
        ?.querySelector(".selected")
        ?.scrollIntoView({ block: "nearest" });
    });
    return () => cancelAnimationFrame(frame);
  }, []);

  return (
    <>
      <div
        className="option-picker-backdrop"
        role="presentation"
        onMouseDown={onClose}
      />
      <section
        className="option-picker"
        role="dialog"
        aria-modal="true"
        aria-labelledby="option-picker-title"
        onMouseDown={(event) => event.stopPropagation()}
      >
        <div className="option-picker-head">
          <h2 id="option-picker-title">{title}</h2>
          <button
            type="button"
            className="option-picker-close"
            onClick={onClose}
            aria-label="닫기"
          >
            <DashboardIcon name="x" size={14} />
          </button>
        </div>
        <ul className="option-picker-list" ref={listRef}>
          {options.map((option) => (
            <li key={option.value}>
              <button
                type="button"
                className={option.value === value ? "selected" : ""}
                onClick={() => onSelect(option.value)}
              >
                {option.label}
              </button>
            </li>
          ))}
        </ul>
      </section>
    </>
  );
}

export default OptionPicker;
