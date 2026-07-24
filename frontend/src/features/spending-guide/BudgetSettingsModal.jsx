import { useState } from "react";
import {
  updateIncomeType,
  updatePayday,
  updateSalary,
  updateWorkSchedule,
} from "../../api/spendingGuideApi";
import DashboardIcon from "../../components/common/DashboardIcon";
import { toNumber } from "./spendingGuideUtils";
import IncomeProfileFields from "./components/IncomeProfileFields";

function BudgetSettingsModal({ summary, onClose, onSaved }) {
  const initialIncomeType =
    summary.incomeType === "HOURLY" ? "HOURLY" : "SALARY";
  const [incomeType, setIncomeType] = useState(initialIncomeType);
  const isHourly = incomeType === "HOURLY";
  const [salary, setSalary] = useState(
    String(Number(summary.salaryAmount || 0)),
  );
  const [hourlyWage, setHourlyWage] = useState(
    String(Number(summary.hourlyWage || 0)),
  );
  const [workSchedule, setWorkSchedule] = useState(
    (summary.workSchedule || []).map((item) => ({
      dayOfWeek: item.dayOfWeek,
      hours: String(item.hours),
    })),
  );
  const [applyFrom, setApplyFrom] = useState("CURRENT_CYCLE");
  const [payday, setPayday] = useState(String(summary.payday || ""));
  const [error, setError] = useState("");
  const [isSubmitting, setIsSubmitting] = useState(false);

  const hasIncompleteHours =
    isHourly &&
    (workSchedule.length === 0 ||
      workSchedule.some((item) => !toNumber(item.hours)));

  const handleSubmit = async (event) => {
    event.preventDefault();
    setError("");
    setIsSubmitting(true);
    try {
      const typeChanged = incomeType !== initialIncomeType;
      const incomeUpdate = typeChanged
        ? updateIncomeType({
            incomeType,
            salaryAmount: isHourly ? undefined : Number(salary),
            hourlyWage: isHourly ? Number(hourlyWage) : undefined,
            workSchedule: isHourly
              ? workSchedule.map((item) => ({
                  dayOfWeek: item.dayOfWeek,
                  hours: Number(item.hours),
                }))
              : undefined,
            applyFrom,
          })
        : isHourly
          ? updateWorkSchedule({
              hourlyWage: Number(hourlyWage),
              workSchedule: workSchedule.map((item) => ({
                dayOfWeek: item.dayOfWeek,
                hours: Number(item.hours),
              })),
              applyFrom,
            })
          : updateSalary({ amount: Number(salary), applyFrom });
      const paydayChanged = Number(payday) !== Number(summary.payday);
      await Promise.all([
        incomeUpdate,
        paydayChanged ? updatePayday({ payday: Number(payday) }) : null,
      ]);
      await onSaved();
      onClose();
    } catch (requestError) {
      setError(
        requestError.response?.data?.message ||
          "소득 정보를 저장하지 못했습니다.",
      );
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <div
      className="spending-modal-backdrop"
      role="presentation"
      onMouseDown={onClose}
    >
      <section
        className="spending-modal spending-budget-modal"
        role="dialog"
        aria-modal="true"
        aria-labelledby="budget-settings-title"
        onMouseDown={(event) => event.stopPropagation()}
      >
        <div className="spending-modal-head">
          <div>
            <h2 id="budget-settings-title">소득 정보 변경</h2>
            <p>
              {isHourly ? "시급·근무시간" : "월급"} 정보와 적용 시점을 선택해
              변경해요.
            </p>
          </div>
          <button
            type="button"
            className="spending-modal-close"
            onClick={onClose}
            aria-label="닫기"
          >
            ×
          </button>
        </div>
        <form className="spending-entry-form" onSubmit={handleSubmit}>
          <IncomeProfileFields
            incomeType={incomeType}
            onIncomeTypeChange={setIncomeType}
            showTypeToggle
            salaryAmount={salary}
            onSalaryAmountChange={setSalary}
            hourlyWage={hourlyWage}
            onHourlyWageChange={setHourlyWage}
            workSchedule={workSchedule}
            onWorkScheduleChange={setWorkSchedule}
          />
          <label className="wide">
            <span>적용 시점 *</span>
            <select
              value={applyFrom}
              onChange={(event) => setApplyFrom(event.target.value)}
            >
              <option value="CURRENT_CYCLE">이번 급여 주기부터</option>
              <option value="NEXT_CYCLE">다음 급여 주기부터</option>
            </select>
          </label>
          <div className="spending-form-notice wide">
            <DashboardIcon name="info" size={17} />
            <span>
              현재 주기에 적용하면 남은 예산과 하루 권장액이 즉시 다시
              계산됩니다.
            </span>
          </div>
          <label className="wide">
            <span>급여일 *</span>
            <input
              type="number"
              min="1"
              max="31"
              value={payday}
              onChange={(event) => setPayday(event.target.value)}
            />
          </label>
          <div className="spending-form-notice wide">
            <DashboardIcon name="info" size={17} />
            <span>
              급여일 변경은 항상 다음 급여 주기부터 적용돼요. 진행 중인 이번
              주기는 그대로 유지됩니다.
              {summary.pendingPayday != null &&
                ` (다음 주기부터 ${summary.pendingPayday}일로 변경 예정)`}
            </span>
          </div>
          {error && (
            <div className="spending-form-error wide">
              <DashboardIcon name="info" size={16} />
              {error}
            </div>
          )}
          <button
            type="submit"
            className="spending-primary wide"
            disabled={isSubmitting || hasIncompleteHours}
          >
            {isSubmitting ? "저장 중..." : "소득 정보 저장"}
          </button>
        </form>
      </section>
    </div>
  );
}

export default BudgetSettingsModal;
