import { useState } from "react";

const getCharacterCount = (value) => Array.from(value.trim()).length;

function CalendarScheduleForm({ initialSchedule, defaultDate, isSaving, error, onSubmit, onCancel }) {
  const [title, setTitle] = useState(initialSchedule?.title || "");
  const [scheduleDate, setScheduleDate] = useState(initialSchedule?.eventDate || defaultDate);
  const [validationError, setValidationError] = useState("");
  const titleCount = getCharacterCount(title);
  const editing = Boolean(initialSchedule);
  const handleSubmit = async (event) => {
    event.preventDefault();
    const normalizedTitle = title.trim();
    const count = getCharacterCount(title);
    if (!normalizedTitle) {
      setValidationError("일정 제목을 입력해 주세요.");
      return;
    }
    if (count > 20) {
      setValidationError("일정 제목은 20자 이하로 입력해 주세요.");
      return;
    }
    if (!scheduleDate) {
      setValidationError("일정 날짜를 선택해 주세요.");
      return;
    }
    setValidationError("");
    await onSubmit({
      title: normalizedTitle,
      scheduleDate,
    });
  };

  return (
    <form className="calendar-schedule-form" onSubmit={handleSubmit}>
      <label>
        <span>일정 제목</span>
        <input
          type="text"
          value={title}
          onChange={(event) => setTitle(event.target.value)}
          placeholder="예: 지원서 제출"
          disabled={isSaving}
        />
      </label>
      <div className={`calendar-title-count${titleCount > 20 ? " is-error" : ""}`}>{titleCount} / 20</div>
      <label>
        <span>일정 날짜</span>
        <input
          type="date"
          value={scheduleDate}
          onChange={(event) => setScheduleDate(event.target.value)}
          disabled={isSaving}
        />
      </label>
      {(validationError || error) && <p className="calendar-form-error">{validationError || error}</p>}
      <div className="calendar-form-actions">
        <button type="submit" disabled={isSaving || titleCount > 20 || !title.trim()}>
          {isSaving ? "저장 중..." : editing ? "일정 수정" : "일정 등록"}
        </button>
        {onCancel && (
          <button type="button" className="secondary" onClick={onCancel} disabled={isSaving}>
            취소
          </button>
        )}
      </div>
    </form>
  );
}

export default CalendarScheduleForm;
