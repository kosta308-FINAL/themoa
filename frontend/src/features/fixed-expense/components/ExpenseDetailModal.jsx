import { useState } from "react";
import DashboardIcon from "../../../components/common/DashboardIcon";
import { getApiErrorMessage } from "../../../utils/apiError";
import {
  cancelFixedExpense,
  confirmMissedPayment,
  getMissedPaymentCandidates,
  updateFixedExpense,
} from "../../../api/fixedExpenseApi";
import {
  formatAmount,
  formatWon,
  METHOD_LABEL,
  paymentStatusBadge,
  serviceInitial,
  toneForId,
} from "../fixedExpenseUtils";

const PAY_DAYS = Array.from({ length: 31 }, (_, index) => index + 1);
const WON = new Intl.NumberFormat("ko-KR", { maximumFractionDigits: 0 });

function MissedPaymentSection({ fixedExpenseId, onConfirmed }) {
  const [state, setState] = useState({
    items: null,
    error: "",
    loading: false,
  });
  const [confirmingId, setConfirmingId] = useState(null);

  const load = async () => {
    setState({ items: null, error: "", loading: true });
    try {
      const items = await getMissedPaymentCandidates(fixedExpenseId);
      setState({ items, error: "", loading: false });
    } catch (error) {
      setState({
        items: null,
        error: getApiErrorMessage(error, "결제내역을 불러오지 못했어요."),
        loading: false,
      });
    }
  };

  const confirm = async (transactionId) => {
    setConfirmingId(transactionId);
    try {
      await confirmMissedPayment(fixedExpenseId, transactionId);
      await onConfirmed("결제를 확인했어요.");
      setState({ items: null, error: "", loading: false });
    } catch (error) {
      setState((current) => ({
        ...current,
        error: getApiErrorMessage(error, "결제 확인에 실패했어요."),
      }));
    } finally {
      setConfirmingId(null);
    }
  };

  return (
    <div className="fx-missed-payment">
      <div className="fx-missed-payment-head">
        <h4>결제내역이 안 보이나요?</h4>
        <button type="button" className="fx-ghost-button" onClick={load}>
          {state.loading ? "찾는 중..." : "결제내역 확인"}
        </button>
      </div>
      {state.error && <p className="fx-form-error-text">{state.error}</p>}
      {state.items &&
        (state.items.length ? (
          <div className="fx-missed-payment-list">
            {state.items.map((transaction) => (
              <div className="fx-missed-payment-row" key={transaction.id}>
                <div>
                  <strong>
                    {transaction.merchantDisplayName ||
                      transaction.merchantNameRaw}
                  </strong>
                  <span>
                    {transaction.usedDate} · {formatWon(transaction.netAmount)}
                  </span>
                </div>
                <button
                  type="button"
                  className="fx-secondary-button"
                  disabled={confirmingId === transaction.id}
                  onClick={() => confirm(transaction.id)}
                >
                  {confirmingId === transaction.id
                    ? "확인 중..."
                    : "이 거래예요"}
                </button>
              </div>
            ))}
          </div>
        ) : (
          <p className="fx-missed-payment-empty">
            비슷한 미태깅 거래를 찾지 못했어요.
          </p>
        ))}
    </div>
  );
}

function ExpenseDetailModal({
  expense,
  hasCardConnection,
  onClose,
  onChanged,
}) {
  const [isEditing, setIsEditing] = useState(false);
  const [editForm, setEditForm] = useState({
    amount: String(Number(expense.expectedAmount)),
    currency: expense.expectedCurrency,
    payDay: String(expense.expectedPayDay),
  });
  const [error, setError] = useState("");
  const [isSaving, setIsSaving] = useState(false);
  const [isCanceling, setIsCanceling] = useState(false);

  const handleAmount = (event) =>
    setEditForm((current) => ({
      ...current,
      amount: event.target.value.replace(/[^\d.]/g, ""),
    }));

  const handleSave = async (event) => {
    event.preventDefault();
    setError("");
    setIsSaving(true);
    try {
      await updateFixedExpense(expense.id, {
        expectedAmount: Number(editForm.amount),
        expectedCurrency: editForm.currency,
        expectedPayDay: Number(editForm.payDay),
      });
      await onChanged("금액·결제일을 수정했어요.");
      onClose();
    } catch (requestError) {
      setError(getApiErrorMessage(requestError, "수정하지 못했어요."));
    } finally {
      setIsSaving(false);
    }
  };

  const handleCancel = async () => {
    if (!window.confirm(`'${expense.name}' 고정지출을 해지할까요?`)) return;
    setIsCanceling(true);
    try {
      await cancelFixedExpense(expense.id);
      await onChanged("고정지출을 해지했어요.");
      onClose();
    } catch (requestError) {
      setError(getApiErrorMessage(requestError, "해지하지 못했어요."));
      setIsCanceling(false);
    }
  };

  return (
    <div
      className="fx-modal-backdrop"
      role="presentation"
      onMouseDown={onClose}
    >
      <section
        className="fx-modal sm"
        role="dialog"
        aria-modal="true"
        aria-labelledby="fx-detail-title"
        onMouseDown={(event) => event.stopPropagation()}
      >
        <div className="fx-modal-head">
          <div>
            <h2 id="fx-detail-title">고정지출 상세</h2>
            <p>등록 정보와 다음 결제 예정일을 확인하세요.</p>
          </div>
          <button
            type="button"
            className="fx-modal-close"
            onClick={onClose}
            aria-label="닫기"
          >
            <DashboardIcon name="x" />
          </button>
        </div>
        <div className="fx-modal-body">
          <div className="fx-detail-hero">
            <span className={`fx-service-icon ${toneForId(expense.id)}`}>
              {serviceInitial(expense.merchantAliasName || expense.name)}
            </span>
            <h3>{expense.name}</h3>
            <strong>
              {formatAmount(expense.expectedAmount, expense.expectedCurrency)}
            </strong>
            {expense.expectedCurrency !== "KRW" && (
              <span className="fx-detail-krw">
                예상 {formatAmount(expense.expectedAmountKrw, "KRW")}
              </span>
            )}
          </div>

          {isEditing ? (
            <form className="fx-form-grid fx-edit-form" onSubmit={handleSave}>
              <div className="fx-field">
                <label htmlFor="fx-edit-amount">예상 금액 *</label>
                <input
                  id="fx-edit-amount"
                  inputMode="decimal"
                  value={
                    editForm.amount && editForm.currency === "KRW"
                      ? WON.format(Number(editForm.amount))
                      : editForm.amount
                  }
                  onChange={handleAmount}
                  required
                />
              </div>
              <div className="fx-field">
                <label htmlFor="fx-edit-payday">매월 결제일 *</label>
                <select
                  id="fx-edit-payday"
                  value={editForm.payDay}
                  onChange={(event) =>
                    setEditForm((current) => ({
                      ...current,
                      payDay: event.target.value,
                    }))
                  }
                  required
                >
                  {PAY_DAYS.map((day) => (
                    <option key={day} value={day}>
                      {day}일
                    </option>
                  ))}
                </select>
              </div>
              {error && <p className="fx-form-error-text full">{error}</p>}
              <div className="fx-modal-actions full">
                <button
                  type="button"
                  className="fx-ghost-button"
                  onClick={() => setIsEditing(false)}
                >
                  취소
                </button>
                <button
                  type="submit"
                  className="fx-primary-button"
                  disabled={isSaving}
                >
                  {isSaving ? "저장 중..." : "저장하기"}
                </button>
              </div>
            </form>
          ) : (
            <>
              <div className="fx-detail-list">
                <div className="fx-detail-row">
                  <span>카테고리</span>
                  <strong>{expense.categoryName}</strong>
                </div>
                <div className="fx-detail-row">
                  <span>결제수단</span>
                  <strong>{METHOD_LABEL[expense.paymentMethod]}</strong>
                </div>
                <div className="fx-detail-row">
                  <span>결제일</span>
                  <strong>매월 {expense.expectedPayDay}일</strong>
                </div>
                {expense.paymentStatus && (
                  <div className="fx-detail-row">
                    <span>이번 달 이행 상태</span>
                    <strong>
                      <span
                        className={`fx-status-chip ${paymentStatusBadge(expense.paymentStatus)?.tone || ""}`}
                      >
                        {paymentStatusBadge(expense.paymentStatus)?.label}
                      </span>
                    </strong>
                  </div>
                )}
              </div>

              {expense.paymentMethod === "CARD" && hasCardConnection && (
                <MissedPaymentSection
                  fixedExpenseId={expense.id}
                  onConfirmed={onChanged}
                />
              )}

              {error && <p className="fx-form-error-text">{error}</p>}

              <div className="fx-detail-danger">
                <p>
                  금액과 결제일은 수정할 수 있어요.
                  <br />
                  해지해도 지난 이행 기록은 유지됩니다.
                </p>
                <button
                  type="button"
                  className="fx-danger-button"
                  disabled={isCanceling}
                  onClick={handleCancel}
                >
                  {isCanceling ? "해지 중..." : "고정지출 해지"}
                </button>
              </div>
            </>
          )}
        </div>
        {!isEditing && (
          <div className="fx-modal-actions">
            <button type="button" className="fx-ghost-button" onClick={onClose}>
              닫기
            </button>
            <button
              type="button"
              className="fx-primary-button"
              onClick={() => setIsEditing(true)}
            >
              금액·결제일 수정
            </button>
          </div>
        )}
      </section>
    </div>
  );
}

export default ExpenseDetailModal;
