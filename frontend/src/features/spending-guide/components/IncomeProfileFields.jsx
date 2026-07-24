import { useState } from "react";
import DashboardIcon from "../../../components/common/DashboardIcon";
import HourPicker from "../../../components/common/HourPicker";
import {
  AVG_WEEKS_PER_MONTH,
  TAX_OPTIONS,
  estimateNetPay,
  formatHourLabel,
  parseDecimalHours,
  roundToThousand,
  withHour,
  withMinute,
} from "../../../utils/workScheduleEstimate";
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
  const [taxType, setTaxType] = useState("INSURANCE");
  const [openHourDay, setOpenHourDay] = useState(null);

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
        item.dayOfWeek === dayOfWeek ? { ...item, hours } : item,
      ),
    );
  };

  const setDayHour = (dayOfWeek, currentHours, hour) =>
    setDayHours(dayOfWeek, withHour(currentHours, hour));

  const setDayMinute = (dayOfWeek, currentHours, minute) =>
    setDayHours(dayOfWeek, withMinute(currentHours, minute));

  const grossEstimate = roundToThousand(
    workSchedule.reduce((sum, item) => sum + toNumber(item.hours), 0) *
      toNumber(hourlyWage) *
      AVG_WEEKS_PER_MONTH,
  );
  const netEstimate = estimateNetPay(
    workSchedule.reduce((sum, item) => sum + toNumber(item.hours), 0) *
      toNumber(hourlyWage) *
      AVG_WEEKS_PER_MONTH,
    taxType,
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
                ).map((day) => {
                  const currentHours = workSchedule.find(
                    (item) => item.dayOfWeek === day.value,
                  )?.hours;
                  const { minute } = parseDecimalHours(currentHours);
                  return (
                    <li key={day.value}>
                      <span>{day.label}요일</span>
                      <div className="spending-hour-field">
                        <button
                          type="button"
                          className="spending-hour-trigger"
                          aria-expanded={openHourDay === day.value}
                          onClick={() =>
                            setOpenHourDay((current) =>
                              current === day.value ? null : day.value,
                            )
                          }
                        >
                          {formatHourLabel(currentHours)}
                          <DashboardIcon name="chevron-down" size={13} />
                        </button>
                        {openHourDay === day.value && (
                          <HourPicker
                            value={currentHours}
                            onChange={(hour) =>
                              setDayHour(day.value, currentHours, hour)
                            }
                            onClose={() => setOpenHourDay(null)}
                          />
                        )}
                      </div>
                      <div className="spending-minute-toggle">
                        <button
                          type="button"
                          className={minute === 0 ? "selected" : ""}
                          onClick={() =>
                            setDayMinute(day.value, currentHours, 0)
                          }
                        >
                          0분
                        </button>
                        <button
                          type="button"
                          className={minute === 30 ? "selected" : ""}
                          onClick={() =>
                            setDayMinute(day.value, currentHours, 30)
                          }
                        >
                          30분
                        </button>
                      </div>
                    </li>
                  );
                })}
              </ul>
            )}
            <div className="spending-weekday-tax">
              <span>예상 공제 *</span>
              <div className="spending-segmented-toggle spending-segmented-toggle-tax">
                {TAX_OPTIONS.map((option) => (
                  <button
                    type="button"
                    key={option.value}
                    className={taxType === option.value ? "selected" : ""}
                    onClick={() => setTaxType(option.value)}
                  >
                    {option.label}
                  </button>
                ))}
              </div>
            </div>
            {grossEstimate > 0 && (
              <div className="spending-weekday-estimate-block">
                <p className="spending-weekday-estimate">
                  <DashboardIcon name="info" size={14} />이 스케줄이면 이번 주기
                  약 {WON.format(netEstimate)}원 받아요 (세전{" "}
                  {WON.format(grossEstimate)}원 ·{" "}
                  {TAX_OPTIONS.find((o) => o.value === taxType)?.label} 적용)
                </p>
                <p className="spending-weekday-estimate-note">
                  1,000원 단위 근사치예요. 주휴수당 등은 반영하지 않으니, 실제
                  금액은 급여 주기의 요일 수와 입금액에 따라 달라질 수 있어요.
                </p>
              </div>
            )}
          </div>
        </>
      )}
    </>
  );
}

export default IncomeProfileFields;
