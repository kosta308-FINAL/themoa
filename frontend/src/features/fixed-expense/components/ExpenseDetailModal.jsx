import { useEffect, useState } from "react";
import DashboardIcon from "../../../components/common/DashboardIcon";
import ServiceIcon from "./ServiceIcon";
import { getApiErrorMessage } from "../../../utils/apiError";
import {
  cancelFixedExpense,
  confirmManualPayment,
  confirmMissedPayment,
  getFixedExpensePaymentConfirmation,
  getMissedPaymentCandidates,
  undoFixedExpensePaymentConfirmation,
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

function ConfirmModal({
  title,
  message,
  confirmLabel,
  isDanger,
  isBusy,
  onConfirm,
  onCancel,
}) {
  return (
    <div
      className="fx-confirm-backdrop"
      role="presentation"
      onMouseDown={onCancel}
    >
      <section
        className="fx-confirm-modal"
        role="alertdialog"
        aria-modal="true"
        aria-labelledby="fx-confirm-title"
        onMouseDown={(event) => event.stopPropagation()}
      >
        <h3 id="fx-confirm-title">{title}</h3>
        <p>{message}</p>
        <div className="fx-confirm-actions">
          <button
            type="button"
            className="fx-ghost-button"
            disabled={isBusy}
            onClick={onCancel}
          >
            취소
          </button>
          <button
            type="button"
            className={isDanger ? "fx-danger-button" : "fx-primary-button"}
            disabled={isBusy}
            onClick={onConfirm}
          >
            {confirmLabel}
          </button>
        </div>
      </section>
    </div>
  );
}

function MissedPaymentSection({
  fixedExpenseId,
  fixedExpenseName,
  onConfirmed,
}) {
  const [state, setState] = useState({
    items: null,
    error: "",
    loading: false,
  });
  const [confirmingId, setConfirmingId] = useState(null);
  const [confirmedPayment, setConfirmedPayment] = useState(undefined);
  const [isUndoing, setIsUndoing] = useState(false);
  const [pendingConfirm, setPendingConfirm] = useState(null);
  const [pendingUndo, setPendingUndo] = useState(false);

  useEffect(() => {
    let active = true;

    getFixedExpensePaymentConfirmation(fixedExpenseId)
      .then((payment) => {
        if (active) setConfirmedPayment(payment || null);
      })
      .catch(() => {
        if (active) setConfirmedPayment(null);
      });

    return () => {
      active = false;
    };
  }, [fixedExpenseId]);

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

  const requestConfirm = (transaction) => setPendingConfirm(transaction);

  const performConfirm = async () => {
    const transaction = pendingConfirm;
    if (!transaction) return;
    setPendingConfirm(null);
    setConfirmingId(transaction.id);
    try {
      await confirmMissedPayment(fixedExpenseId, transaction.id);
      const payment = await getFixedExpensePaymentConfirmation(fixedExpenseId);
      setConfirmedPayment(payment || null);
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

  const requestUndo = () => setPendingUndo(true);

  const performUndo = async () => {
    setPendingUndo(false);
    setIsUndoing(true);
    try {
      await undoFixedExpensePaymentConfirmation(
        fixedExpenseId,
        confirmedPayment.transactionId,
      );
      setConfirmedPayment(null);
      setState({ items: null, error: "", loading: false });
      await onConfirmed("결제 연결을 해제했어요.");
    } catch (error) {
      setState((current) => ({
        ...current,
        error: getApiErrorMessage(error, "결제 연결을 해제하지 못했어요."),
      }));
    } finally {
      setIsUndoing(false);
    }
  };

  return (
    <div className="fx-missed-payment">
      <div className="fx-missed-payment-head">
        <h4>
          {confirmedPayment
            ? confirmedPayment.userConfirmedMatch
              ? "이번 달 연결된 결제"
              : "이번 달 결제, 자동으로 확인했어요"
            : "결제내역이 안 보이나요?"}
        </h4>
        {confirmedPayment === null && (
          <button type="button" className="fx-ghost-button" onClick={load}>
            {state.loading ? "찾는 중..." : "결제내역 확인"}
          </button>
        )}
      </div>
      {state.error && <p className="fx-form-error-text">{state.error}</p>}
      {confirmedPayment && (
        <div className="fx-missed-payment-list">
          <div className="fx-missed-payment-row">
            <div>
              <strong>{confirmedPayment.merchantName}</strong>
              <span>
                {confirmedPayment.usedDate} ·{" "}
                {formatWon(confirmedPayment.amount)}
              </span>
            </div>
            <button
              type="button"
              className="fx-ghost-button"
              disabled={isUndoing}
              onClick={requestUndo}
            >
              {isUndoing ? "해제 중..." : "연결 해제"}
            </button>
          </div>
          {!confirmedPayment.userConfirmedMatch && (
            <p className="fx-missed-payment-empty">
              카드 결제내역과 자동으로 대조해 연결했어요. 다른 구독과 헷갈려
              잘못 연결됐다면 해제 후 아래에서 직접 다시 연결할 수 있어요.
            </p>
          )}
        </div>
      )}
      {confirmedPayment === null &&
        state.items &&
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
                  onClick={() => requestConfirm(transaction)}
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
      {pendingConfirm && (
        <ConfirmModal
          title="이 거래로 연결할까요?"
          message={`${pendingConfirm.merchantDisplayName || pendingConfirm.merchantNameRaw} · ${pendingConfirm.usedDate} · ${formatWon(pendingConfirm.netAmount)}\n\n이 거래를 '${fixedExpenseName}' 결제로 연결할까요?\n필요한 경우 이 가맹점 표기가 학습에 반영됩니다.`}
          confirmLabel="연결할게요"
          isBusy={confirmingId === pendingConfirm.id}
          onConfirm={performConfirm}
          onCancel={() => setPendingConfirm(null)}
        />
      )}
      {pendingUndo && (
        <ConfirmModal
          title="결제 연결을 해제할까요?"
          message={
            confirmedPayment?.userConfirmedMatch
              ? `'${confirmedPayment?.merchantName}' 결제 연결을 해제할까요?\n이번 연결에서 새로 학습된 가맹점 정보도 함께 되돌립니다.`
              : `'${confirmedPayment?.merchantName}' 결제 연결을 해제할까요?\n해제하면 이번 달은 다시 미확인 상태가 되고, 결제내역에서 직접 연결할 수 있어요.`
          }
          confirmLabel="해제할게요"
          isDanger
          isBusy={isUndoing}
          onConfirm={performUndo}
          onCancel={() => setPendingUndo(false)}
        />
      )}
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
  const [isEditingName, setIsEditingName] = useState(false);
  const [nameDraft, setNameDraft] = useState(expense.name);
  const [nameError, setNameError] = useState("");
  const [error, setError] = useState("");
  const [confirmError, setConfirmError] = useState("");
  const [cancelError, setCancelError] = useState("");
  const [isSavingName, setIsSavingName] = useState(false);
  const [isSaving, setIsSaving] = useState(false);
  const [isCanceling, setIsCanceling] = useState(false);
  const [isConfirmingCancel, setIsConfirmingCancel] = useState(false);
  const [isConfirmingPayment, setIsConfirmingPayment] = useState(false);

  const handleAmount = (event) =>
    setEditForm((current) => ({
      ...current,
      amount: event.target.value.replace(/[^\d.]/g, ""),
    }));

  const handleNameSave = async (event) => {
    event.preventDefault();
    const nextName = nameDraft.trim();
    if (!nextName) {
      setNameError("고정지출 이름을 입력해 주세요.");
      return;
    }
    if (!expense.expectedPayDay) {
      setNameError("결제일을 먼저 설정한 뒤 이름을 수정해 주세요.");
      return;
    }
    if (nextName === expense.name) {
      setIsEditingName(false);
      return;
    }

    setNameError("");
    setIsSavingName(true);
    try {
      await updateFixedExpense(expense.id, {
        name: nextName,
        expectedAmount: Number(expense.expectedAmount),
        expectedCurrency: expense.expectedCurrency,
        expectedPayDay: expense.expectedPayDay,
      });
      setNameDraft(nextName);
      await onChanged("이름을 수정했어요.");
      setIsEditingName(false);
    } catch (requestError) {
      setNameError(
        getApiErrorMessage(requestError, "이름을 수정하지 못했어요."),
      );
    } finally {
      setIsSavingName(false);
    }
  };

  const handleNameEditCancel = () => {
    setNameDraft(expense.name);
    setNameError("");
    setIsEditingName(false);
  };

  const handleDetailEditStart = () => {
    handleNameEditCancel();
    setIsEditing(true);
  };

  const handleSave = async (event) => {
    event.preventDefault();
    setError("");
    setIsSaving(true);
    try {
      await updateFixedExpense(expense.id, {
        name: expense.name,
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

  const handleConfirmPayment = async () => {
    setConfirmError("");
    setIsConfirmingPayment(true);
    try {
      await confirmManualPayment(expense.id);
      await onChanged("결제 처리했어요.");
      onClose();
    } catch (requestError) {
      setConfirmError(
        getApiErrorMessage(requestError, "결제 처리에 실패했어요."),
      );
    } finally {
      setIsConfirmingPayment(false);
    }
  };

  const handleCancel = async () => {
    setCancelError("");
    setIsCanceling(true);
    try {
      await cancelFixedExpense(expense.id);
      await onChanged("고정지출을 해지했어요.");
      onClose();
    } catch (requestError) {
      setCancelError(getApiErrorMessage(requestError, "해지하지 못했어요."));
      setIsCanceling(false);
      setIsConfirmingCancel(false);
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
            <ServiceIcon tone={toneForId(expense.id)}>
              {serviceInitial(expense.merchantAliasName || expense.name)}
            </ServiceIcon>
            {isEditingName ? (
              <form className="fx-detail-name-edit" onSubmit={handleNameSave}>
                <input
                  className="fx-detail-name-input"
                  value={nameDraft}
                  onChange={(event) => setNameDraft(event.target.value)}
                  aria-label="고정지출 이름"
                  maxLength={100}
                  autoFocus
                  required
                />
                <button
                  type="submit"
                  className="fx-detail-name-action confirm"
                  disabled={isSavingName}
                  aria-label="이름 저장"
                >
                  <DashboardIcon name="check" size={16} />
                </button>
                <button
                  type="button"
                  className="fx-detail-name-action"
                  disabled={isSavingName}
                  onClick={handleNameEditCancel}
                  aria-label="이름 수정 취소"
                >
                  <DashboardIcon name="x" size={16} />
                </button>
              </form>
            ) : (
              <div className="fx-detail-name">
                <h3>{expense.name}</h3>
                {!isEditing && (
                  <button
                    type="button"
                    className="fx-detail-name-button"
                    onClick={() => {
                      setNameDraft(expense.name);
                      setNameError("");
                      setIsEditingName(true);
                    }}
                    aria-label="고정지출 이름 수정"
                  >
                    <DashboardIcon name="edit" size={15} />
                  </button>
                )}
              </div>
            )}
            {nameError && <p className="fx-detail-name-error">{nameError}</p>}
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
                  {expense.expectedPayDay ? (
                    <strong>매월 {expense.expectedPayDay}일</strong>
                  ) : (
                    <span className="fx-detail-unset">
                      미설정
                      <button
                        type="button"
                        className="fx-detail-unset-cta"
                        onClick={handleDetailEditStart}
                      >
                        설정하기
                      </button>
                    </span>
                  )}
                </div>
                {!expense.expectedPayDay && (
                  <p className="fx-detail-hint">
                    예·적금 가입으로 자동 등록된 고정지출이라 결제일이 아직
                    없어요. 아래에서 직접 입력할 수 있어요.
                  </p>
                )}
                {expense.paymentMethod === "CARD" && expense.paymentStatus && (
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
                  fixedExpenseName={expense.name}
                  onConfirmed={onChanged}
                />
              )}

              {expense.paymentMethod === "CARD" &&
                !hasCardConnection &&
                (expense.paymentStatus === "DUE_SOON" ||
                  expense.paymentStatus === "MISSED") && (
                  <div className="fx-manual-confirm">
                    <div className="fx-manual-confirm-head">
                      <h4>이번 달 결제, 하셨나요?</h4>
                      <p>
                        카드내역과 자동으로 대조할 수 없어 직접 확인이 필요해요.
                      </p>
                    </div>
                    <button
                      type="button"
                      className="fx-secondary-button"
                      disabled={isConfirmingPayment}
                      onClick={handleConfirmPayment}
                    >
                      {isConfirmingPayment ? "처리 중..." : "결제처리"}
                    </button>
                    {confirmError && (
                      <p className="fx-form-error-text">{confirmError}</p>
                    )}
                  </div>
                )}

              <div className="fx-detail-danger">
                {isConfirmingCancel ? (
                  <>
                    <p>'{expense.name}' 고정지출을 정말 해지하시겠어요?</p>
                    <div className="fx-detail-danger-actions">
                      <button
                        type="button"
                        className="fx-ghost-button"
                        disabled={isCanceling}
                        onClick={() => setIsConfirmingCancel(false)}
                      >
                        아니오
                      </button>
                      <button
                        type="button"
                        className="fx-danger-button"
                        disabled={isCanceling}
                        onClick={handleCancel}
                      >
                        {isCanceling ? "해지 중..." : "네, 해지할게요"}
                      </button>
                    </div>
                  </>
                ) : (
                  <>
                    <p>
                      금액과 결제일은 수정할 수 있어요.
                      <br />
                      해지해도 지난 이행 기록은 유지됩니다.
                    </p>
                    <button
                      type="button"
                      className="fx-danger-button"
                      onClick={() => setIsConfirmingCancel(true)}
                    >
                      고정지출 해지
                    </button>
                  </>
                )}
                {cancelError && (
                  <p className="fx-form-error-text">{cancelError}</p>
                )}
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
              onClick={handleDetailEditStart}
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
