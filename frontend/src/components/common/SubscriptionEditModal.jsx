import { useEffect, useState } from "react";
import { createPortal } from "react-dom";
import { updateSubscription } from "../../api/savingsSubscriptionApi";
import { getApiErrorMessage } from "../../utils/apiError";
import { stripCommas, withCommas } from "../../utils/numberFormat";
// 가입 등록 모달과 동일한 폼 스타일(ss-*)을 그대로 재사용한다.
import "./SavingsSubscriptionModal.css";

/**
 * 가입한 예·적금 수정 모달. 목록 응답에 값이 다 있어 그걸 그대로 초기값으로 채운다.
 * 월납입액·적용금리·기간·가입일을 고쳐 PUT으로 저장한다(우대조건 충족은 카드에서 따로 토글).
 */
function SubscriptionEditModal({ subscription, onClose, onSaved }) {
  const [monthlyAmount, setMonthlyAmount] = useState(
    String(subscription.monthlyAmount ?? ""),
  );
  const [appliedRate, setAppliedRate] = useState(
    String(subscription.appliedRate ?? ""),
  );
  const [termMonth, setTermMonth] = useState(
    String(subscription.termMonth ?? ""),
  );
  const [startDate, setStartDate] = useState(subscription.startDate ?? "");
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState("");

  useEffect(() => {
    const handleKeyDown = (event) => {
      if (event.key === "Escape") onClose();
    };
    document.addEventListener("keydown", handleKeyDown);
    return () => document.removeEventListener("keydown", handleKeyDown);
  }, [onClose]);

  const monthlyValid = Number(monthlyAmount) >= 10000;
  const termValid = Number(termMonth) >= 1;

  const handleSubmit = async (event) => {
    event.preventDefault();
    if (submitting || !monthlyValid || !termValid) return;
    setSubmitting(true);
    setError("");
    try {
      await updateSubscription(subscription.id, {
        monthlyAmount: Number(monthlyAmount),
        appliedRate: Number(appliedRate) || 0,
        termMonth: Number(termMonth),
        startDate,
      });
      onSaved?.("가입 상품을 수정했어요.");
      onClose();
    } catch (saveError) {
      setError(getApiErrorMessage(saveError, "수정에 실패했어요."));
    } finally {
      setSubmitting(false);
    }
  };

  return createPortal(
    <div
      className="ss-backdrop"
      role="presentation"
      onClick={(event) => {
        if (event.target === event.currentTarget) onClose();
      }}
    >
      <div className="ss-modal" role="dialog" aria-label="가입 상품 수정">
        <div className="ss-head">
          <strong>가입 상품 수정</strong>
          <button type="button" onClick={onClose} aria-label="닫기">
            ×
          </button>
        </div>

        <form onSubmit={handleSubmit}>
          <div className="ss-body">
            <div className="ss-product">
              <strong>{subscription.productName}</strong>
              <span>{subscription.companyName}</span>
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
                <span>가입기간 (개월)</span>
                <input
                  type="number"
                  min="1"
                  value={termMonth}
                  onChange={(event) => setTermMonth(event.target.value)}
                  required
                />
              </label>

              <label className="ss-field">
                <span>적용금리 (%)</span>
                <input
                  type="number"
                  step="0.01"
                  min="0"
                  value={appliedRate}
                  onChange={(event) => setAppliedRate(event.target.value)}
                />
              </label>
            </div>

            <label className="ss-field">
              <span>가입일</span>
              <input
                type="date"
                value={startDate}
                onChange={(event) => setStartDate(event.target.value)}
                required
              />
            </label>

            {error && <p className="ss-error">{error}</p>}
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
              disabled={submitting || !monthlyValid || !termValid}
            >
              {submitting ? "저장 중..." : "저장"}
            </button>
          </div>
        </form>
      </div>
    </div>,
    document.body,
  );
}

export default SubscriptionEditModal;
