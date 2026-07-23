import { useState } from "react";
import { formatDateKey } from "../calendarDateUtils";
import CalendarScheduleForm from "./CalendarScheduleForm";

const EVENT_META = {
  FIXED_EXPENSE_DUE: { label: "고정지출", className: "calendar-event-fixed-expense-due" },
  POLICY_START: { label: "신청 시작", className: "calendar-event-policy-start" },
  POLICY_DEADLINE: { label: "신청 마감", className: "calendar-event-policy-deadline" },
  POLICY_SINGLE_DAY: { label: "신청일", className: "calendar-event-policy-single-day" },
  USER_SCHEDULE: { label: "내 일정", className: "calendar-event-user-schedule" },
};

const WON = new Intl.NumberFormat("ko-KR");

const formatPanelDate = (date) =>
  `${date.getFullYear()}년 ${date.getMonth() + 1}월 ${date.getDate()}일`;

function CalendarDayPanel({
  selectedDate,
  selectedDateKey,
  events,
  isSaving,
  mutationError,
  onCreate,
  onUpdate,
  onDelete,
}) {
  const [formMode, setFormMode] = useState("closed");
  const [editingEvent, setEditingEvent] = useState(null);

  const openCreate = () => {
    setEditingEvent(null);
    setFormMode("create");
  };

  const openEdit = (event) => {
    setEditingEvent(event);
    setFormMode("edit");
  };

  const closeForm = () => {
    setEditingEvent(null);
    setFormMode("closed");
  };

  const handleSubmit = async (payload) => {
    if (editingEvent) {
      await onUpdate(editingEvent.sourceId, payload);
    } else {
      await onCreate(payload);
    }
    closeForm();
  };

  const handleDelete = async (event) => {
    if (!window.confirm("일정을 삭제할까요?")) return;
    await onDelete(event.sourceId);
  };

  return (
    <aside className="calendar-day-panel">
      <div className="calendar-day-panel-header">
        <div>
          <span>선택 날짜</span>
          <h2>{formatPanelDate(selectedDate)}</h2>
        </div>
        <button type="button" onClick={openCreate}>
          일정 추가
        </button>
      </div>

      {events.length === 0 ? (
        <div className="calendar-empty-state">등록된 일정이 없어요.</div>
      ) : (
        <ul className="calendar-day-event-list">
          {events.map((event) => {
            const meta = EVENT_META[event.eventType];
            return (
              <li key={event.eventKey}>
                <span className={`calendar-event-badge ${meta.className}`}>{meta.label}</span>
                <strong>{event.title}</strong>
                {event.amount != null && <p>{WON.format(Number(event.amount))}원</p>}
                {event.editable && (
                  <div className="calendar-event-actions">
                    <button type="button" onClick={() => openEdit(event)} disabled={isSaving}>
                      수정
                    </button>
                    <button type="button" onClick={() => handleDelete(event)} disabled={isSaving}>
                      삭제
                    </button>
                  </div>
                )}
              </li>
            );
          })}
        </ul>
      )}

      {formMode !== "closed" && (
        <CalendarScheduleForm
          key={`${formMode}-${editingEvent?.eventKey || selectedDateKey}`}
          initialSchedule={editingEvent}
          defaultDate={formatDateKey(selectedDate)}
          isSaving={isSaving}
          error={mutationError}
          onSubmit={handleSubmit}
          onCancel={closeForm}
        />
      )}
      {formMode === "closed" && mutationError && <p className="calendar-form-error">{mutationError}</p>}
    </aside>
  );
}

export default CalendarDayPanel;
