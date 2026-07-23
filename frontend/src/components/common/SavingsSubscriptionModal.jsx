import { useEffect, useMemo, useState } from "react";
import { createPortal } from "react-dom";
import {
  createSubscription,
  getSubscriptionDraft,
} from "../../api/savingsSubscriptionApi";
import { getApiErrorMessage } from "../../utils/apiError";
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
  const appliedRate = round2(baseRate + bonusSum);
  const overMax = maxRate != null && appliedRate > maxRate;

  const toggleCondition = (index) => {
    setConditions((prev) =>
      prev.map((condition, i) =>
        i === index ? { ...condition, met: !condition.met } : condition,
      ),
    );
  };

  const handleSubmit = async (event) => {
    event.preventDefault();
    if (submitting) return;
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
                    type="number"
                    min="0"
                    step="10000"
                    value={monthlyAmount}
                    onChange={(event) => setMonthlyAmount(event.target.value)}
                    required
                  />
                  <em>원</em>
                </div>
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

              <div className="ss-applied">
                <span>내 금리</span>
                <strong>
                  기본 {baseRate}% + 우대 {round2(bonusSum)}% = {appliedRate}%
                </strong>
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
              <button type="submit" className="ss-submit" disabled={submitting}>
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
