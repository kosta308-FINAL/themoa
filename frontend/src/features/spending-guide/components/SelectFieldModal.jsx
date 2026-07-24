import { useState } from "react";
import DashboardIcon from "../../../components/common/DashboardIcon";

/**
 * 네이티브 select 대신 사용하는 커스텀 선택 필드.
 * 트리거 버튼 아래로 옵션 목록 드롭다운을 펼쳐 선택하게 합니다.
 */
function SelectFieldModal({
  label,
  value,
  options,
  onChange,
  placeholder = "선택",
  wide = false,
  disabled = false,
}) {
  const [isOpen, setIsOpen] = useState(false);
  const selected = options.find(
    (option) => String(option.value) === String(value),
  );

  return (
    <label className={`spending-select-field${wide ? " wide" : ""}`}>
      <span>{label}</span>
      <button
        type="button"
        className="spending-select-trigger"
        aria-expanded={isOpen}
        onClick={() => setIsOpen((open) => !open)}
        disabled={disabled}
      >
        <span className={selected ? "" : "placeholder"}>
          {selected ? selected.label : placeholder}
        </span>
        <DashboardIcon name="chevron-down" size={16} />
      </button>
      {isOpen && (
        <ul className="spending-select-dropdown" role="listbox">
          {options.map((option) => (
            <li key={option.value}>
              <button
                type="button"
                role="option"
                aria-selected={String(option.value) === String(value)}
                className={
                  String(option.value) === String(value) ? "selected" : ""
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
    </label>
  );
}

export default SelectFieldModal;
