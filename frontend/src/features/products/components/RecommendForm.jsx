import { useState } from "react";
import { stripCommas, withCommas } from "../../../utils/numberFormat";

const EMPLOYMENT_OPTIONS = [
  { value: "직장인", label: "직장인" },
  { value: "프리랜서", label: "프리랜서" },
  { value: "무관", label: "무관" },
];

const RISK_OPTIONS = [
  { value: "STABLE", label: "안정형" },
  { value: "NEUTRAL", label: "중립형" },
  { value: "AGGRESSIVE", label: "공격형" },
];

const PERIOD_OPTIONS = [
  { value: "SINGLE", label: "초단기 (1개월)" },
  { value: "SHORT", label: "단기 (6개월)" },
  { value: "MID", label: "중기 (1~2년)" },
  { value: "LONG", label: "장기 (3년)" },
];

const toIntOrNull = (value) => {
  if (value === "" || value === null || value === undefined) {
    return null;
  }
  const parsed = Number(value);
  return Number.isNaN(parsed) ? null : parsed;
};

const FALLBACK_DEPOSIT_WON = "300000";

/**
 * 추천 입력 폼. 값은 내부 상태로만 관리하고, 제출 시 백엔드 RecommendRequest 형태의
 * payload로 변환해 onSubmit으로 넘긴다. 저축목표 모드가 '없음'이면 목표 값은 항상 null로 보낸다.
 *
 * 월소득·월 납입가능금액은 서버 기본값(가입 시 월급, 소비내역 연동 잉여금)으로 시작하되
 * 사용자가 그대로 수정할 수 있다. defaults가 준비된 뒤에 마운트되므로 초기값으로만 쓰면 된다.
 */
function RecommendForm({ loading, defaults, onSubmit }) {
  const [age, setAge] = useState("26");
  const [income, setIncome] = useState(
    defaults?.monthlyIncomeManwon != null
      ? String(defaults.monthlyIncomeManwon)
      : "",
  );
  const [employment, setEmployment] = useState("직장인");
  const [risk, setRisk] = useState("STABLE");
  const [deposit, setDeposit] = useState(
    defaults?.monthlyDepositWon != null
      ? String(defaults.monthlyDepositWon)
      : FALLBACK_DEPOSIT_WON,
  );
  const [period, setPeriod] = useState("SHORT");
  const [goalMode, setGoalMode] = useState("none"); // none | set
  const [goalAmount, setGoalAmount] = useState("");
  const [goalMonths, setGoalMonths] = useState("");
  const [lowIncome, setLowIncome] = useState(false);
  const [acceptCondition, setAcceptCondition] = useState(false);
  const [needLiquidity, setNeedLiquidity] = useState(false);
  // 결과를 넓게 보고 싶을 때 입력 패널을 접을 수 있다.
  const [collapsed, setCollapsed] = useState(false);

  const hasGoal = goalMode === "set";
  // 백엔드가 @Min(10000)으로 검증하므로 화면에서도 미달이면 추천을 막는다.
  const depositValid = Number(deposit) >= 10000;

  const handleSubmit = (event) => {
    event.preventDefault();
    if (loading || !depositValid) {
      return;
    }
    onSubmit({
      age: toIntOrNull(age) ?? 0,
      monthlyIncomeManwon: toIntOrNull(income),
      employmentType: employment,
      lowIncome,
      riskType: risk,
      preferredPeriod: period,
      monthlyDepositWon: toIntOrNull(deposit) ?? 0,
      acceptCondition,
      needLiquidity,
      goalAmountWon: hasGoal ? toIntOrNull(goalAmount) : null,
      goalMonths: hasGoal ? toIntOrNull(goalMonths) : null,
    });
  };

  return (
    <form
      className={`rec-form${collapsed ? " rec-form-collapsed" : ""}`}
      onSubmit={handleSubmit}
    >
      <button
        type="button"
        className="rec-form-toggle"
        onClick={() => setCollapsed((prev) => !prev)}
        aria-expanded={!collapsed}
      >
        <span>내 정보 입력</span>
        <span className="rec-form-caret">{collapsed ? "▸" : "▾"}</span>
      </button>

      {collapsed ? (
        <p className="rec-form-collapsed-hint">
          입력값이 그대로 유지돼요. 다시 펼쳐서 수정할 수 있어요.
        </p>
      ) : (
        <div className="rec-form-body">
          <label className="rec-field">
            <span>나이</span>
            <input
              type="number"
              min="1"
              max="120"
              value={age}
              onChange={(event) => setAge(event.target.value)}
            />
          </label>

          <label className="rec-field">
            <span>월소득 (만원, 선택)</span>
            <input
              type="text"
              inputMode="numeric"
              value={withCommas(income)}
              onChange={(event) => setIncome(stripCommas(event.target.value))}
            />
          </label>

          <label className="rec-field">
            <span>취업유형</span>
            <select
              value={employment}
              onChange={(event) => setEmployment(event.target.value)}
            >
              {EMPLOYMENT_OPTIONS.map((option) => (
                <option key={option.value} value={option.value}>
                  {option.label}
                </option>
              ))}
            </select>
          </label>

          <label className="rec-field">
            <span>위험성향</span>
            <select
              value={risk}
              onChange={(event) => setRisk(event.target.value)}
            >
              {RISK_OPTIONS.map((option) => (
                <option key={option.value} value={option.value}>
                  {option.label}
                </option>
              ))}
            </select>
          </label>

          <label className="rec-field">
            <span>월 납입가능금액 (원)</span>
            <input
              type="text"
              inputMode="numeric"
              value={withCommas(deposit)}
              onChange={(event) => setDeposit(stripCommas(event.target.value))}
            />
            {!depositValid && (
              <small className="rec-field-error">
                월 납입금액은 10,000원 이상 입력해 주세요.
              </small>
            )}
          </label>

          <div className="rec-field">
            <span>저축목표</span>
            <div className="rec-goal-toggle">
              <button
                type="button"
                className={goalMode === "none" ? "active" : ""}
                onClick={() => setGoalMode("none")}
              >
                목표 없음
              </button>
              <button
                type="button"
                className={goalMode === "set" ? "active" : ""}
                onClick={() => setGoalMode("set")}
              >
                직접 입력
              </button>
            </div>
          </div>

          {hasGoal ? (
            <div className="rec-goal-fields">
              <label className="rec-field">
                <span>저축목표 금액 (원)</span>
                <input
                  type="text"
                  inputMode="numeric"
                  placeholder="예: 5,000,000"
                  value={withCommas(goalAmount)}
                  onChange={(event) =>
                    setGoalAmount(stripCommas(event.target.value))
                  }
                />
              </label>
              <label className="rec-field">
                <span>저축목표 기간 (개월, 최대 36)</span>
                <input
                  type="number"
                  min="1"
                  max="36"
                  placeholder="예: 12"
                  value={goalMonths}
                  onChange={(event) => setGoalMonths(event.target.value)}
                />
              </label>
            </div>
          ) : (
            <label className="rec-field">
              <span>선호 가입기간</span>
              <select
                value={period}
                onChange={(event) => setPeriod(event.target.value)}
              >
                {PERIOD_OPTIONS.map((option) => (
                  <option key={option.value} value={option.value}>
                    {option.label}
                  </option>
                ))}
              </select>
            </label>
          )}

          <div className="rec-checks">
            <label className="rec-check">
              <input
                type="checkbox"
                checked={lowIncome}
                onChange={(event) => setLowIncome(event.target.checked)}
              />
              차상위계층
            </label>
            <label className="rec-check">
              <input
                type="checkbox"
                checked={acceptCondition}
                onChange={(event) => setAcceptCondition(event.target.checked)}
              />
              우대조건 감수 가능
            </label>
            <label className="rec-check">
              <input
                type="checkbox"
                checked={needLiquidity}
                onChange={(event) => setNeedLiquidity(event.target.checked)}
              />
              중간에 뺄 수도 있어요 (유동성 중요)
            </label>
          </div>

          <button
            type="submit"
            className="rec-submit"
            disabled={loading || !depositValid}
          >
            {loading ? "추천 중…" : "추천받기"}
          </button>
        </div>
      )}
    </form>
  );
}

export default RecommendForm;
