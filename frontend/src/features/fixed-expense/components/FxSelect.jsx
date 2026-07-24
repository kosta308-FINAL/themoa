import { useEffect, useRef, useState } from "react";
import DashboardIcon from "../../../components/common/DashboardIcon";

/** 네이티브 select의 OS 기본 팝업 대신 앱 톤에 맞춘 커스텀 드롭다운(fx-merchant-picker와 같은 패턴). */
function FxSelect({
  id,
  ariaLabel,
  value,
  onChange,
  options,
  placeholder = "선택",
  disabled = false,
  className = "",
}) {
  const [isOpen, setIsOpen] = useState(false);
  const boxRef = useRef(null);

  useEffect(() => {
    if (!isOpen) return undefined;
    const handleOutsideClick = (event) => {
      if (boxRef.current && !boxRef.current.contains(event.target)) {
        setIsOpen(false);
      }
    };
    document.addEventListener("mousedown", handleOutsideClick);
    return () => document.removeEventListener("mousedown", handleOutsideClick);
  }, [isOpen]);

  const selected = options.find(
    (option) => String(option.value) === String(value),
  );

  return (
    <div className={`fx-select ${className}`} ref={boxRef}>
      <button
        type="button"
        id={id}
        className="fx-select-trigger"
        disabled={disabled}
        onClick={() => setIsOpen((current) => !current)}
        aria-haspopup="listbox"
        aria-expanded={isOpen}
        aria-label={ariaLabel}
      >
        <span className={selected ? "" : "fx-select-placeholder"}>
          {selected ? selected.label : placeholder}
        </span>
        <DashboardIcon
          name="chevron-down"
          size={14}
          className="fx-select-caret"
        />
      </button>
      {isOpen && (
        <ul className="fx-select-list" role="listbox">
          {options.map((option) => (
            <li
              key={option.value}
              role="option"
              aria-selected={String(option.value) === String(value)}
            >
              <button
                type="button"
                className={
                  String(option.value) === String(value) ? "is-selected" : ""
                }
                onClick={() => {
                  onChange(option.value);
                  setIsOpen(false);
                }}
              >
                {option.label}
              </button>
            </li>
          ))}
        </ul>
      )}
    </div>
  );
}

export default FxSelect;
