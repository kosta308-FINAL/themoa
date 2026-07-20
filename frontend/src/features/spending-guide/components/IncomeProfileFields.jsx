import DashboardIcon from "../../../components/common/DashboardIcon";
import { WON, toNumber } from "../spendingGuideUtils";

const WEEKDAYS = [
  { value: "MONDAY", label: "월" },
  { value: "TUESDAY", label: "화" },
  { value: "WEDNESDAY", label: "수" },
  { value: "THURSDAY", label: "목" },
  { value: "FRIDAY", label: "금" },
  { value: "SATURDAY", label: "토" },
  { value: "SUNDAY", label: "일" },
];

const AVG_WEEKS_PER_MONTH = 4.345;

const digits = (value) => value.replace(/\D/g, "").slice(0, 12);

/**
 * 소득유형(월급제/알바 시급제) 입력. 최초 설정 화면(S-00A)과 예산 기준 변경 모달이 함께 쓴다.
 * showTypeToggle=false면 유형 전환 UI 없이 현재 유형의 입력 필드만 보여준다.
 */
function IncomeProfileFields({
  incomeType,
  onIncomeTypeChange,
  showTypeToggle = true,
  salaryAmount,
  onSalaryAmountChange,
  hourlyWage,
  onHourlyWageChange,
  workSchedule,
  onWorkScheduleChange,
}) {
  const toggleDay = (dayOfWeek) => {
    const exists = workSchedule.some((item) => item.dayOfWeek === dayOfWeek);
    if (exists) {
      onWorkScheduleChange(
        workSchedule.filter((item) => item.dayOfWeek !== dayOfWeek),
      );
      return;
    }
    onWorkScheduleChange([...workSchedule, { dayOfWeek, hours: "" }]);
  };

  const setDayHours = (dayOfWeek, hours) => {
    onWorkScheduleChange(
      workSchedule.map((item) =>
        item.dayOfWeek === dayOfWeek
          ? { ...item, hours: digits(hours).slice(0, 2) }
          : item,
      ),
    );
  };

  const monthlyEstimate = Math.round(
    workSchedule.reduce((sum, item) => sum + toNumber(item.hours), 0) *
      toNumber(hourlyWage) *
      AVG_WEEKS_PER_MONTH,
  );

  return (
    <>
      {showTypeToggle && (
        <label className="wide">
          <span>소득 방식 *</span>
          <div className="spending-segmented-toggle">
            <button
              type="button"
              className={incomeType === "SALARY" ? "selected" : ""}
              onClick={() => onIncomeTypeChange("SALARY")}
            >
              월급제
            </button>
            <button
              type="button"
              className={incomeType === "HOURLY" ? "selected" : ""}
              onClick={() => onIncomeTypeChange("HOURLY")}
            >
              알바(시급제)
            </button>
          </div>
        </label>
      )}

      {incomeType === "SALARY" ? (
        <label className={showTypeToggle ? "" : "wide"}>
          <span>월 실수령액 *</span>
          <div className="spending-input-suffix">
            <input
              inputMode="numeric"
              value={salaryAmount ? WON.format(Number(salaryAmount)) : ""}
              onChange={(event) =>
                onSalaryAmountChange(digits(event.target.value))
              }
              placeholder="0"
              required
            />
            <em>원</em>
          </div>
        </label>
      ) : (
        <>
          <label className={showTypeToggle ? "" : "wide"}>
            <span>시급 *</span>
            <div className="spending-input-suffix">
              <input
                inputMode="numeric"
                value={hourlyWage ? WON.format(Number(hourlyWage)) : ""}
                onChange={(event) =>
                  onHourlyWageChange(digits(event.target.value))
                }
                placeholder="0"
                required
              />
              <em>원</em>
            </div>
          </label>
          <div className="wide spending-weekday-field">
            <span>근무 요일 *</span>
            <div className="spending-weekday-picker">
              {WEEKDAYS.map((day) => (
                <button
                  type="button"
                  key={day.value}
                  className={
                    workSchedule.some((item) => item.dayOfWeek === day.value)
                      ? "selected"
                      : ""
                  }
                  onClick={() => toggleDay(day.value)}
                >
                  {day.label}
                </button>
              ))}
            </div>
            {workSchedule.length > 0 && (
              <ul className="spending-weekday-hours">
                {WEEKDAYS.filter((day) =>
                  workSchedule.some((item) => item.dayOfWeek === day.value),
                ).map((day) => (
                  <li key={day.value}>
                    <span>{day.label}요일</span>
                    <input
                      inputMode="numeric"
                      value={
                        workSchedule.find(
                          (item) => item.dayOfWeek === day.value,
                        )?.hours || ""
                      }
                      onChange={(event) =>
                        setDayHours(day.value, event.target.value)
                      }
                      placeholder="0"
                      required
                    />
                    <em>시간</em>
                  </li>
                ))}
              </ul>
            )}
            {monthlyEstimate > 0 && (
              <p className="spending-weekday-estimate">
                <DashboardIcon name="info" size={14} />이 스케줄이면 매달 약{" "}
                {WON.format(monthlyEstimate)}원(참고용, 실제 금액은 급여 주기의
                요일 수에 따라 달라져요)
              </p>
            )}
          </div>
        </>
      )}
    </>
  );
}

export default IncomeProfileFields;
