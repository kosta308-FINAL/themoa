import { useMemo, useState } from "react";
import {
  createCardConnection,
  getCardIssuers,
  setupSpendingGuide,
} from "../../../api/spendingGuideApi";
import DashboardIcon from "../../../components/common/DashboardIcon";
import { errorMessage } from "../spendingGuideUtils";
import IncomeProfileFields from "./IncomeProfileFields";

function SpendingGuideSetup({ onComplete, onCardConnected }) {
  const [step, setStep] = useState(1);
  const [incomeType, setIncomeType] = useState("SALARY");
  const [salaryAmount, setSalaryAmount] = useState("");
  const [hourlyWage, setHourlyWage] = useState("");
  const [workSchedule, setWorkSchedule] = useState([]);
  const [payday, setPayday] = useState("");
  const [isPaydayOpen, setIsPaydayOpen] = useState(false);
  const [issuers, setIssuers] = useState([]);
  const [isIssuerLoading, setIsIssuerLoading] = useState(false);
  const [showBirthDate, setShowBirthDate] = useState(false);
  const [cardForm, setCardForm] = useState({
    organization: "",
    loginId: "",
    loginPassword: "",
    cardNo: "",
    cardPassword: "",
    birthDate: "",
  });
  const [error, setError] = useState("");
  const [isSubmitting, setIsSubmitting] = useState(false);

  const selectedIssuer = useMemo(
    () =>
      issuers.find((issuer) => issuer.organization === cardForm.organization),
    [cardForm.organization, issuers],
  );

  const isHourly = incomeType === "HOURLY";
  const canSubmitStep1 = isHourly
    ? hourlyWage &&
      workSchedule.length > 0 &&
      workSchedule.every((item) => item.hours) &&
      payday
    : salaryAmount && payday;

  const handleSalarySubmit = async (event) => {
    event.preventDefault();
    setError("");
    setIsSubmitting(true);
    try {
      await setupSpendingGuide({
        incomeType,
        salaryAmount: isHourly ? null : Number(salaryAmount),
        hourlyWage: isHourly ? Number(hourlyWage) : null,
        workSchedule: isHourly
          ? workSchedule.map((item) => ({
              dayOfWeek: item.dayOfWeek,
              hours: Number(item.hours),
            }))
          : null,
        payday: Number(payday),
      });
      setStep(2);
    } catch (requestError) {
      setError(
        errorMessage(requestError, "소비가이드 설정을 저장하지 못했습니다."),
      );
    } finally {
      setIsSubmitting(false);
    }
  };

  const openCardStep = async () => {
    setStep(3);
    setError("");
    if (issuers.length) return;
    setIsIssuerLoading(true);
    try {
      setIssuers((await getCardIssuers()) || []);
    } catch (requestError) {
      setError(
        errorMessage(requestError, "지원 카드사를 불러오지 못했습니다."),
      );
    } finally {
      setIsIssuerLoading(false);
    }
  };

  const updateCardForm = (key) => (event) => {
    setCardForm((current) => ({ ...current, [key]: event.target.value }));
    setError("");
  };

  const handleCardSubmit = async (event) => {
    event.preventDefault();
    setError("");
    setIsSubmitting(true);
    try {
      const connection = await createCardConnection({
        organization: cardForm.organization,
        loginId: cardForm.loginId,
        loginPassword: cardForm.loginPassword,
        cardNo: cardForm.cardNo || null,
        cardPassword: cardForm.cardPassword || null,
        birthDate: cardForm.birthDate || null,
      });
      await onCardConnected(connection);
    } catch (requestError) {
      if (
        requestError.response?.data?.code ===
        "CARD_CONNECTION_BIRTHDATE_REQUIRED"
      ) {
        setShowBirthDate(true);
      }
      setError(
        errorMessage(
          requestError,
          "카드를 연결하지 못했습니다. 입력 정보를 확인해주세요.",
        ),
      );
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <section className="spending-setup-shell">
      <div
        className="spending-setup-progress"
        aria-label={`설정 ${step}/3단계`}
      >
        {[1, 2, 3].map((number) => (
          <span className={number <= step ? "active" : ""} key={number} />
        ))}
      </div>

      {step === 1 && (
        <>
          <div className="spending-setup-copy">
            <span className="spending-setup-icon">
              <DashboardIcon name="wallet" size={28} />
            </span>
            <h2>소비 가이드를 시작해 볼까요?</h2>
            <p>월급과 급여일을 기준으로 매일 쓸 수 있는 금액을 계산해드려요.</p>
          </div>
          <form className="spending-setup-form" onSubmit={handleSalarySubmit}>
            <IncomeProfileFields
              incomeType={incomeType}
              onIncomeTypeChange={setIncomeType}
              salaryAmount={salaryAmount}
              onSalaryAmountChange={setSalaryAmount}
              hourlyWage={hourlyWage}
              onHourlyWageChange={setHourlyWage}
              workSchedule={workSchedule}
              onWorkScheduleChange={setWorkSchedule}
            />
            <label className="spending-payday-field">
              <span>매월 급여일 *</span>
              <button
                className="spending-payday-trigger"
                type="button"
                aria-expanded={isPaydayOpen}
                onClick={() => setIsPaydayOpen((open) => !open)}
              >
                <strong>{payday ? `${payday}일` : "급여일 선택"}</strong>
                <DashboardIcon name="calendar" size={18} />
              </button>
              {isPaydayOpen && (
                <div className="spending-payday-picker">
                  <strong>매월 급여일 선택</strong>
                  <div>
                    {Array.from({ length: 31 }, (_, index) => {
                      const day = index + 1;
                      return (
                        <button
                          className={Number(payday) === day ? "selected" : ""}
                          type="button"
                          key={day}
                          onClick={() => {
                            setPayday(String(day));
                            setIsPaydayOpen(false);
                          }}
                        >
                          {day}
                        </button>
                      );
                    })}
                  </div>
                </div>
              )}
            </label>
            <div className="spending-setup-wide">
              <p className="spending-setup-help">
                29~31일이 없는 달에는 마지막 날을 기준으로 계산해요. 주말이나
                공휴일로 실제 입금일이 달라져도 회사가 정한 급여일을
                입력해주세요.
              </p>
              <div className="spending-setup-notice">
                <span>
                  <DashboardIcon name="info" size={17} />
                </span>
                저축 목표는 나중에 설정할 수 있으며, 미설정 시 0원으로 계산돼요.
              </div>
              {error && (
                <div className="spending-form-error">
                  <DashboardIcon name="info" size={16} />
                  {error}
                </div>
              )}
              <button
                className="spending-setup-action"
                type="submit"
                disabled={isSubmitting || !canSubmitStep1}
              >
                {isSubmitting ? "저장 중..." : "다음"}
              </button>
            </div>
          </form>
        </>
      )}

      {step === 2 && (
        <>
          <div className="spending-setup-copy">
            <span className="spending-setup-icon">
              <DashboardIcon name="receipt" size={28} />
            </span>
            <h2>소비내역을 어떻게 기록할까요?</h2>
            <p>
              카드를 연결해도 현금과 계좌이체 지출은 언제든 직접 기록할 수
              있어요.
            </p>
          </div>
          <div className="spending-method-cards">
            <button type="button" onClick={openCardStep}>
              <span>
                <DashboardIcon name="card" size={24} />
              </span>
              <strong>카드 내역 자동으로 불러오기</strong>
              <p>카드 결제내역과 카테고리를 자동으로 정리해드려요.</p>
            </button>
            <button type="button" onClick={onComplete}>
              <span>
                <DashboardIcon name="receipt" size={24} />
              </span>
              <strong>직접 입력해서 시작하기</strong>
              <p>
                카드 연결 없이 현금·카드·계좌이체 내역을 직접 기록할 수 있어요.
              </p>
            </button>
          </div>
        </>
      )}

      {step === 3 && (
        <>
          <div className="spending-setup-copy">
            <span className="spending-setup-icon">
              <DashboardIcon name="card" size={28} />
            </span>
            <h2>사용 중인 카드를 연결해주세요</h2>
            <p>카드사 계정 하나를 연결하면 해당 계정의 카드를 모두 불러와요.</p>
          </div>
          <form
            className="spending-setup-card-form"
            onSubmit={handleCardSubmit}
          >
            <label>
              <span>카드사 *</span>
              <select
                value={cardForm.organization}
                onChange={updateCardForm("organization")}
                disabled={isIssuerLoading}
                required
              >
                <option value="" disabled>
                  {isIssuerLoading
                    ? "카드사를 불러오는 중..."
                    : "카드사를 선택해주세요"}
                </option>
                {issuers.map((issuer) => (
                  <option value={issuer.organization} key={issuer.organization}>
                    {issuer.name}
                  </option>
                ))}
              </select>
            </label>
            <label>
              <span>카드사 로그인 아이디 *</span>
              <input
                value={cardForm.loginId}
                onChange={updateCardForm("loginId")}
                autoComplete="username"
                placeholder="카드사 홈페이지 아이디"
                required
              />
            </label>
            <label>
              <span>카드사 로그인 비밀번호 *</span>
              <input
                type="password"
                value={cardForm.loginPassword}
                onChange={updateCardForm("loginPassword")}
                autoComplete="current-password"
                placeholder="카드사 홈페이지 비밀번호"
                required
              />
            </label>
            {selectedIssuer?.requiresCardCredentials && (
              <>
                <label>
                  <span>카드번호 *</span>
                  <input
                    value={cardForm.cardNo}
                    onChange={updateCardForm("cardNo")}
                    inputMode="numeric"
                    placeholder="카드번호"
                    required
                  />
                </label>
                <label>
                  <span>카드 비밀번호 앞 2자리 *</span>
                  <input
                    type="password"
                    value={cardForm.cardPassword}
                    onChange={updateCardForm("cardPassword")}
                    inputMode="numeric"
                    maxLength={2}
                    placeholder="앞 2자리"
                    required
                  />
                </label>
              </>
            )}
            {showBirthDate && (
              <label>
                <span>생년월일(주민등록번호) *</span>
                <input
                  value={cardForm.birthDate}
                  onChange={updateCardForm("birthDate")}
                  inputMode="numeric"
                  placeholder="본인 확인을 위해 추가 정보가 필요해요"
                  required
                />
              </label>
            )}
            <div className="spending-setup-notice">
              <span>
                <DashboardIcon name="info" size={17} />
              </span>
              입력한 카드사 아이디와 비밀번호는 카드 연결에만 사용되며 저장되지
              않아요.
            </div>
            {error && (
              <div className="spending-form-error">
                <DashboardIcon name="info" size={16} />
                {error}
              </div>
            )}
            <button
              className="spending-setup-action"
              type="submit"
              disabled={isSubmitting || isIssuerLoading}
            >
              {isSubmitting
                ? "카드사에 연결하고 있어요"
                : showBirthDate
                  ? "다시 연결하기"
                  : "카드 연결하기"}
            </button>
            <button
              className="spending-setup-skip"
              type="button"
              onClick={onComplete}
            >
              우선 직접 입력하기
            </button>
          </form>
        </>
      )}
    </section>
  );
}

export default SpendingGuideSetup;
