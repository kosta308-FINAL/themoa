import { useEffect, useMemo, useState } from "react";
import { createPortal } from "react-dom";
import {
  createSubscription,
  getSubscriptionDraft,
} from "../../api/savingsSubscriptionApi";
import { getApiErrorMessage } from "../../utils/apiError";
import { stripCommas, withCommas } from "../../utils/numberFormat";
import "./SavingsSubscriptionModal.css";

const PRODUCT_TYPE_LABELS = {
  DEPOSIT: "정기예금",
  SAVING: "적금",
  MORTGAGE: "주택담보대출",
  RENT: "전세자금대출",
  CREDIT: "개인신용대출",
};

// 소수점 둘째 자리까지만 — 금리는 3.45%처럼 표기하고 부동소수점 오차를 없앤다.
const round2 = (value) => Math.round(value * 100) / 100;

const won = (value) => `${Math.round(value).toLocaleString("ko-KR")}원`;

/**
 * 화면에서 바로 보여줄 대략적인 세전 만기금액(단리 기준).
 * 정확한 값은 등록 후 백엔드가 계산해 목록에 내려주고, 여기선 미리보기용 추정치다.
 * - 적금(월납입): 원금 = 월납입×개월, 이자 = 월납입×연이율×(n(n+1)/2)/12
 * - 예금(일시예치): 원금 = 입력액, 이자 = 원금×연이율×개월/12
 */
const estimateMaturity = (productType, monthlyAmount, termMonth, ratePct) => {
  const amount = Number(monthlyAmount) || 0;
  const months = Number(termMonth) || 0;
  const rate = (Number(ratePct) || 0) / 100;
  if (amount <= 0 || months <= 0) return null;
  if (productType === "DEPOSIT") {
    const principal = amount;
    const interest = principal * rate * (months / 12);
    return { principal, maturity: principal + interest };
  }
  const principal = amount * months;
  const interest = (amount * rate * (months * (months + 1))) / 2 / 12;
  return { principal, maturity: principal + interest };
};

const todayString = () => {
  const now = new Date();
  const month = String(now.getMonth() + 1).padStart(2, "0");
  const day = String(now.getDate()).padStart(2, "0");
  return `${now.getFullYear()}-${month}-${day}`;
};

/**
 * 가입한 예·적금 등록 폼. 상품 id로 초안(기본금리·최고금리·기간·우대조건)을 불러와 채운다.
 * 우대조건은 API가 준 목록을 체크리스트로만 렌더하고(직접 입력 X), 사용자는 해당하는 것에 체크만 한다.
 * 내 금리 = 기본금리 + 체크된 우대조건 가산 합. 최고금리를 넘으면 경고만 하고 등록은 막지 않는다.
 */
function SavingsSubscriptionModal({ productId, onClose, onCreated }) {
  const [draft, setDraft] = useState(null);
  const [loading, setLoading] = useState(true);
  const [loadError, setLoadError] = useState("");

  const [monthlyAmount, setMonthlyAmount] = useState(300000);
  const [termMonth, setTermMonth] = useState(null);
  const [startDate, setStartDate] = useState(todayString());
  const [conditions, setConditions] = useState([]);
  const [submitting, setSubmitting] = useState(false);
  const [submitError, setSubmitError] = useState("");

  useEffect(() => {
    let active = true;
    getSubscriptionDraft(productId)
      .then((data) => {
        if (!active) return;
        setDraft(data || null);
        const terms = data?.termMonths || [];
        setTermMonth(terms.length > 0 ? terms[0] : 12);
        // 우대조건은 해당하는 것에 사용자가 직접 체크 — 기본은 모두 미체크.
        setConditions(
          (data?.conditions || []).map((condition) => ({
            description: condition.description || "",
            rateBonus: condition.rateBonus ?? 0,
            met: false,
          })),
        );
      })
      .catch((error) => {
        if (active) {
          setLoadError(
            getApiErrorMessage(error, "상품 정보를 불러오지 못했어요."),
          );
        }
      })
      .finally(() => {
        if (active) setLoading(false);
      });
    return () => {
      active = false;
    };
  }, [productId]);

  useEffect(() => {
    const handleKeyDown = (event) => {
      if (event.key === "Escape") onClose();
    };
    document.addEventListener("keydown", handleKeyDown);
    return () => document.removeEventListener("keydown", handleKeyDown);
  }, [onClose]);

  const baseRate = draft?.baseRate ?? 0;
  const maxRate = draft?.maxRate ?? null;

  const bonusSum = useMemo(
    () =>
      conditions.reduce(
        (sum, condition) =>
          condition.met ? sum + (Number(condition.rateBonus) || 0) : sum,
        0,
      ),
    [conditions],
  );
  // 우대조건 합이 최고금리를 넘어도 상품 최고금리를 넘을 수 없으므로 maxRate로 상한을 건다.
  // (경고는 캡핑 전 원래 합 기준으로 띄운다)
  const rawRate = round2(baseRate + bonusSum);
  const appliedRate = maxRate != null ? Math.min(rawRate, maxRate) : rawRate;
  const overMax = maxRate != null && rawRate > maxRate;

  // 백엔드가 @Min(10000)으로 검증하므로 화면에서도 미달이면 등록을 막는다.
  const monthlyValid = Number(monthlyAmount) >= 10000;

  // 월납입액·기간·내 금리가 바뀔 때마다 예상 만기금액을 다시 계산한다.
  const estimate = useMemo(
    () =>
      estimateMaturity(
        draft?.productType,
        monthlyAmount,
        termMonth,
        appliedRate,
      ),
    [draft?.productType, monthlyAmount, termMonth, appliedRate],
  );

  const toggleCondition = (index) => {
    setConditions((prev) =>
      prev.map((condition, i) =>
        i === index ? { ...condition, met: !condition.met } : condition,
      ),
    );
  };

  const handleSubmit = async (event) => {
    event.preventDefault();
    if (submitting || !monthlyValid) return;
    setSubmitting(true);
    setSubmitError("");
    try {
      // draft의 우대조건에 met(체크여부)만 얹어서 전부 보낸다.
      await createSubscription({
        productId,
        monthlyAmount: Number(monthlyAmount),
        appliedRate,
        termMonth: Number(termMonth),
        startDate,
        conditions: conditions.map((condition) => ({
          description: condition.description,
          rateBonus: Number(condition.rateBonus) || 0,
          met: condition.met,
        })),
      });
      onCreated?.("가입 상품을 등록했어요.");
      onClose();
    } catch (error) {
      setSubmitError(getApiErrorMessage(error, "등록에 실패했어요."));
    } finally {
      setSubmitting(false);
    }
  };

  const termOptions = draft?.termMonths?.length
    ? draft.termMonths
    : [6, 12, 24];

  // 카드(hover transform이 걸린 조상) 안에서 렌더되면 fixed 위치가 어긋나 깜빡이므로
  // body로 포탈시켜 항상 화면 정중앙에 고정되게 한다.
  return createPortal(
    <div
      className="ss-backdrop"
      role="presentation"
      onClick={(event) => {
        if (event.target === event.currentTarget) onClose();
      }}
    >
      <div className="ss-modal" role="dialog" aria-label="가입 적금 등록">
        <div className="ss-head">
          <strong>가입 적금 등록</strong>
          <button type="button" onClick={onClose} aria-label="닫기">
            ×
          </button>
        </div>

        {loading && (
          <div className="ss-body">
            {/* 우대조건은 LLM으로 정리해 내려오므로 몇 초 걸릴 수 있다. */}
            <p className="ss-state">
              상품 정보와 우대조건을 정리하고 있어요.
              <br />
              잠시만 기다려 주세요. (최대 몇 초)
            </p>
          </div>
        )}

        {!loading && loadError && (
          <div className="ss-body">
            <p className="ss-error">{loadError}</p>
          </div>
        )}

        {!loading && !loadError && draft && (
          <form onSubmit={handleSubmit}>
            <div className="ss-body">
              <div className="ss-product">
                <strong>{draft.productName}</strong>
                <span>
                  {draft.companyName}
                  {draft.productType &&
                    ` · ${PRODUCT_TYPE_LABELS[draft.productType] || draft.productType}`}
                </span>
              </div>

              {/* 기본금리 / 최고금리 두 가지를 나란히 보여준다(내 금리는 아래에서 계산). */}
              <div className="ss-rates">
                <div>
                  <span>기본금리</span>
                  <strong>{baseRate}%</strong>
                </div>
                <div>
                  <span>최고금리</span>
                  <strong>{maxRate != null ? `${maxRate}%` : "-"}</strong>
                </div>
              </div>

              <label className="ss-field">
                <span>월 납입액</span>
                <div className="ss-amount">
                  <input
                    type="text"
                    inputMode="numeric"
                    value={withCommas(monthlyAmount)}
                    onChange={(event) =>
                      setMonthlyAmount(stripCommas(event.target.value))
                    }
                    required
                  />
                  <em>원</em>
                </div>
                {!monthlyValid && (
                  <small className="ss-field-error">
                    월 납입금액은 10,000원 이상 입력해 주세요.
                  </small>
                )}
              </label>

              <div className="ss-field-row">
                <label className="ss-field">
                  <span>가입기간</span>
                  <select
                    value={termMonth ?? ""}
                    onChange={(event) => setTermMonth(event.target.value)}
                  >
                    {termOptions.map((month) => (
                      <option key={month} value={month}>
                        {month}개월
                      </option>
                    ))}
                  </select>
                </label>

                <label className="ss-field">
                  <span>가입일</span>
                  <input
                    type="date"
                    value={startDate}
                    onChange={(event) => setStartDate(event.target.value)}
                    required
                  />
                </label>
              </div>

              <div className="ss-conditions">
                <span className="ss-conditions-title">
                  우대조건 (해당하는 것에 체크)
                </span>

                {conditions.length === 0 ? (
                  <p className="ss-conditions-empty">
                    우대조건이 없어요. 기본금리로 등록돼요.
                  </p>
                ) : (
                  <p className="ss-conditions-note">
                    AI가 자동 정리한 조건이에요. 실제 상품 조건과 다를 수 있으니
                    확인 후 체크해 주세요.
                  </p>
                )}

                {conditions.map((condition, index) => (
                  <label className="ss-condition" key={index}>
                    <input
                      type="checkbox"
                      checked={condition.met}
                      onChange={() => toggleCondition(index)}
                    />
                    <span className="ss-condition-desc">
                      {condition.description}
                    </span>
                    <em className="ss-condition-bonus">
                      +{condition.rateBonus}%p
                    </em>
                  </label>
                ))}
              </div>

              <div className="ss-summary">
                <div className="ss-summary-row">
                  <span>내 금리</span>
                  <strong>
                    기본 {baseRate}% + 우대 {round2(bonusSum)}% = {appliedRate}%
                    {overMax && " (최고금리 적용)"}
                  </strong>
                </div>
                {estimate && (
                  <div className="ss-summary-row ss-summary-maturity">
                    <span>예상 만기금액 (세전)</span>
                    <strong>{won(estimate.maturity)}</strong>
                  </div>
                )}
                {estimate && (
                  <p className="ss-summary-note">
                    원금 {won(estimate.principal)} 기준 단순 계산이에요. 실제
                    수령액은 이자과세·상품 방식에 따라 달라질 수 있어요.
                  </p>
                )}
              </div>
              {overMax && (
                <p className="ss-warn">
                  ⚠️ 내 금리가 최고금리({maxRate}%)를 넘어요. 확인 후 등록해
                  주세요.
                </p>
              )}

              {submitError && <p className="ss-error">{submitError}</p>}
            </div>

            <div className="ss-foot">
              <button
                type="button"
                className="ss-cancel"
                onClick={onClose}
                disabled={submitting}
              >
                취소
              </button>
              <button
                type="submit"
                className="ss-submit"
                disabled={submitting || !monthlyValid}
              >
                {submitting ? "등록 중..." : "가입 등록"}
              </button>
            </div>
          </form>
        )}
      </div>
    </div>,
    document.body,
  );
}

export default SavingsSubscriptionModal;
