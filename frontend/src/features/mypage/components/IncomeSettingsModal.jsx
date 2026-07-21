import { useEffect, useState } from "react";
import DashboardIcon from "../../../components/common/DashboardIcon";
import {
  getSpendingGuideSummary,
  setupSpendingGuide,
  updateIncomeType,
  updatePayday,
  updateSalary,
  updateWorkSchedule,
} from "../../../api/spendingGuideApi";
import { getApiErrorMessage } from "../../../utils/apiError";
import { WON, toNumber } from "../mypageUtils";

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

function IncomeSettingsModal({ profile, onClose, onSaved }) {
  const isSetup = profile.payday != null;
  const [incomeType, setIncomeType] = useState(profile.incomeType || "SALARY");
  const isHourly = incomeType === "HOURLY";
  const [salary, setSalary] = useState(String(toNumber(profile.salaryAmount)));
  const [hourlyWage, setHourlyWage] = useState(
    String(toNumber(profile.hourlyWage)),
  );
  const [workSchedule, setWorkSchedule] = useState([]);
  const [payday, setPayday] = useState(String(profile.payday || ""));
  const [pendingPayday, setPendingPayday] = useState(null);
  const [applyFrom, setApplyFrom] = useState("CURRENT_CYCLE");
  const [isLoading, setIsLoading] = useState(isSetup);
  const [error, setError] = useState("");
  const [isSubmitting, setIsSubmitting] = useState(false);

  useEffect(() => {
    if (!isSetup) return;
    let cancelled = false;
    (async () => {
      try {
        const summary = await getSpendingGuideSummary();
        if (cancelled || !summary) return;
        setWorkSchedule(
          (summary.workSchedule || []).map((item) => ({
            dayOfWeek: item.dayOfWeek,
            hours: String(item.hours),
          })),
        );
        setPendingPayday(summary.pendingPayday ?? null);
      } catch {
        /* 실패해도 기존 값으로 계속 진행 */
      } finally {
        if (!cancelled) setIsLoading(false);
      }
    })();
    return () => {
      cancelled = true;
    };
  }, [isSetup]);

  const toggleDay = (dayOfWeek) => {
    const exists = workSchedule.some((item) => item.dayOfWeek === dayOfWeek);
    setWorkSchedule(
      exists
        ? workSchedule.filter((item) => item.dayOfWeek !== dayOfWeek)
        : [...workSchedule, { dayOfWeek, hours: "" }],
    );
  };

  const setDayHours = (dayOfWeek, hours) => {
    setWorkSchedule(
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

  const handleSubmit = async (event) => {
    event.preventDefault();
    setError("");
    setIsSubmitting(true);
    try {
      if (!isSetup) {
        await setupSpendingGuide({
          incomeType,
          salaryAmount: isHourly ? null : Number(salary),
          hourlyWage: isHourly ? Number(hourlyWage) : null,
          workSchedule: isHourly
            ? workSchedule.map((item) => ({
                dayOfWeek: item.dayOfWeek,
                hours: Number(item.hours),
              }))
            : null,
          payday: Number(payday),
        });
      } else {
        const typeChanged = incomeType !== profile.incomeType;
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
        const paydayChanged = Number(payday) !== Number(profile.payday);
        await Promise.all([
          incomeUpdate,
          paydayChanged ? updatePayday({ payday: Number(payday) }) : null,
        ]);
      }
      await onSaved("소득 정보를 저장했어요.");
      onClose();
    } catch (requestError) {
      setError(
        getApiErrorMessage(requestError, "소득 정보를 저장하지 못했어요."),
      );
      setIsSubmitting(false);
    }
  };

  return (
    <div
      className="mp-modal-backdrop"
      role="presentation"
      onMouseDown={onClose}
    >
      <section
        className="mp-modal"
        role="dialog"
        aria-modal="true"
        aria-labelledby="mp-income-title"
        onMouseDown={(event) => event.stopPropagation()}
      >
        <div className="mp-modal-head">
          <div>
            <h2 id="mp-income-title">회원 정보 수정</h2>
            <p>소득유형·월급(시급)·급여일을 확인하고 바꿀 수 있어요.</p>
          </div>
          <button
            type="button"
            className="mp-modal-close"
            onClick={onClose}
            aria-label="닫기"
          >
            ×
          </button>
        </div>

        {isLoading ? (
          <div className="mp-modal-body">
            <p className="mp-empty">불러오는 중이에요...</p>
          </div>
        ) : (
          <form className="mp-inline-form" onSubmit={handleSubmit}>
            <label>
              <span>소득 방식 *</span>
              <div className="mp-segmented-toggle">
                <button
                  type="button"
                  className={incomeType === "SALARY" ? "selected" : ""}
                  onClick={() => setIncomeType("SALARY")}
                >
                  월급제
                </button>
                <button
                  type="button"
                  className={incomeType === "HOURLY" ? "selected" : ""}
                  onClick={() => setIncomeType("HOURLY")}
                >
                  알바(시급제)
                </button>
              </div>
            </label>

            {!isHourly ? (
              <label>
                <span>월 실수령액 *</span>
                <div className="mp-input-suffix">
                  <input
                    inputMode="numeric"
                    value={salary ? WON.format(Number(salary)) : ""}
                    onChange={(event) => setSalary(digits(event.target.value))}
                    placeholder="0"
                    required
                  />
                  <em>원</em>
                </div>
              </label>
            ) : (
              <>
                <label>
                  <span>시급 *</span>
                  <div className="mp-input-suffix">
                    <input
                      inputMode="numeric"
                      value={hourlyWage ? WON.format(Number(hourlyWage)) : ""}
                      onChange={(event) =>
                        setHourlyWage(digits(event.target.value))
                      }
                      placeholder="0"
                      required
                    />
                    <em>원</em>
                  </div>
                </label>
                <div className="mp-weekday-field">
                  <span>근무 요일 *</span>
                  <div className="mp-weekday-picker">
                    {WEEKDAYS.map((day) => (
                      <button
                        type="button"
                        key={day.value}
                        className={
                          workSchedule.some(
                            (item) => item.dayOfWeek === day.value,
                          )
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
                    <ul className="mp-weekday-hours">
                      {WEEKDAYS.filter((day) =>
                        workSchedule.some(
                          (item) => item.dayOfWeek === day.value,
                        ),
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
                    <p className="mp-weekday-estimate">
                      <DashboardIcon name="info" size={13} />이 스케줄이면 매달
                      약 {WON.format(monthlyEstimate)}원(참고용)
                    </p>
                  )}
                </div>
              </>
            )}

            {isSetup && (
              <label>
                <span>적용 시점 *</span>
                <select
                  value={applyFrom}
                  onChange={(event) => setApplyFrom(event.target.value)}
                >
                  <option value="CURRENT_CYCLE">이번 급여 주기부터</option>
                  <option value="NEXT_CYCLE">다음 급여 주기부터</option>
                </select>
              </label>
            )}

            <label>
              <span>급여일 *</span>
              <input
                type="number"
                min="1"
                max="31"
                value={payday}
                onChange={(event) => setPayday(event.target.value)}
                required
              />
            </label>
            {isSetup && (
              <div className="mp-modal-warning">
                <DashboardIcon name="info" size={16} />
                <span>
                  급여일 변경은 항상 다음 급여 주기부터 적용돼요. 진행 중인 이번
                  주기는 그대로 유지됩니다.
                  {pendingPayday != null &&
                    ` (다음 주기부터 ${pendingPayday}일로 변경 예정)`}
                </span>
              </div>
            )}

            {error && (
              <div className="mp-form-error">
                <DashboardIcon name="info" size={16} />
                {error}
              </div>
            )}
            <button
              type="submit"
              className="mp-primary-button"
              disabled={isSubmitting}
            >
              {isSubmitting ? "저장 중..." : "저장하기"}
            </button>
          </form>
        )}
      </section>
    </div>
  );
}

export default IncomeSettingsModal;
