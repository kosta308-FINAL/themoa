import { useEffect, useState } from "react";
import {
  createIncomeAdjustment,
  deleteIncomeAdjustment,
  getIncomeAdjustments,
} from "../../api/spendingGuideApi";
import DashboardIcon from "../../components/common/DashboardIcon";
import { errorMessage, WON, formatDate } from "./spendingGuideUtils";

const digits = (value) => value.replace(/\D/g, "").slice(0, 12);

function IncomeAdjustmentModal({ onClose, onSaved }) {
  const [items, setItems] = useState([]);
  const [isListLoading, setIsListLoading] = useState(true);
  const [listError, setListError] = useState("");
  const [sign, setSign] = useState("PLUS");
  const [amount, setAmount] = useState("");
  const [memo, setMemo] = useState("");
  const [error, setError] = useState("");
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [deletingId, setDeletingId] = useState(null);

  const loadList = async () => {
    setIsListLoading(true);
    try {
      setItems(await getIncomeAdjustments());
      setListError("");
    } catch (requestError) {
      setListError(errorMessage(requestError, "내역을 불러오지 못했습니다."));
    } finally {
      setIsListLoading(false);
    }
  };

  useEffect(() => {
    const run = () => loadList();
    run();
  }, []);

  const handleSubmit = async (event) => {
    event.preventDefault();
    setError("");
    setIsSubmitting(true);
    try {
      await createIncomeAdjustment({
        amount: sign === "MINUS" ? -Number(amount) : Number(amount),
        memo: memo.trim() || null,
      });
      setAmount("");
      setMemo("");
      await Promise.all([loadList(), onSaved()]);
    } catch (requestError) {
      setError(errorMessage(requestError, "수입을 저장하지 못했습니다."));
    } finally {
      setIsSubmitting(false);
    }
  };

  const handleDelete = async (id) => {
    setDeletingId(id);
    try {
      await deleteIncomeAdjustment(id);
      await Promise.all([loadList(), onSaved()]);
    } catch (requestError) {
      setListError(errorMessage(requestError, "삭제하지 못했습니다."));
    } finally {
      setDeletingId(null);
    }
  };

  return (
    <div
      className="spending-modal-backdrop"
      role="presentation"
      onMouseDown={onClose}
    >
      <section
        className="spending-modal spending-income-modal"
        role="dialog"
        aria-modal="true"
        aria-labelledby="income-adjustment-title"
        onMouseDown={(event) => event.stopPropagation()}
      >
        <div className="spending-modal-head">
          <div>
            <h2 id="income-adjustment-title">수입 직접 입력</h2>
            <p>용돈·정부지원금 같은 비정기 수입이나 근무 차액을 더해요.</p>
          </div>
          <button
            type="button"
            className="spending-modal-close"
            onClick={onClose}
            aria-label="닫기"
          >
            <DashboardIcon name="x" />
          </button>
        </div>
        <form className="spending-entry-form" onSubmit={handleSubmit}>
          <label className="wide">
            <span>구분 *</span>
            <div className="spending-segmented-toggle">
              <button
                type="button"
                className={sign === "PLUS" ? "selected" : ""}
                onClick={() => setSign("PLUS")}
              >
                추가 수입 (+)
              </button>
              <button
                type="button"
                className={sign === "MINUS" ? "selected" : ""}
                onClick={() => setSign("MINUS")}
              >
                차액 보정 (−)
              </button>
            </div>
          </label>
          <label className="wide">
            <span>금액 *</span>
            <div className="spending-input-suffix">
              <input
                inputMode="numeric"
                value={amount ? WON.format(Number(amount)) : ""}
                onChange={(event) => setAmount(digits(event.target.value))}
                placeholder="0"
                required
              />
              <em>원</em>
            </div>
          </label>
          <label className="wide">
            <span>메모</span>
            <input
              value={memo}
              onChange={(event) => setMemo(event.target.value)}
              maxLength={255}
              placeholder="예: 생일 용돈, 이번주 추가근무"
            />
          </label>
          {error && (
            <div className="spending-form-error wide">
              <DashboardIcon name="info" size={16} />
              {error}
            </div>
          )}
          <button
            type="submit"
            className="spending-primary wide"
            disabled={isSubmitting || !amount}
          >
            {isSubmitting ? "저장 중..." : "수입 추가하기"}
          </button>
        </form>

        <div className="spending-income-list">
          <strong>이번 급여 주기 수입 보정 내역</strong>
          {isListLoading && (
            <p className="spending-income-list-empty">불러오는 중...</p>
          )}
          {!isListLoading && listError && (
            <p className="spending-income-list-empty">{listError}</p>
          )}
          {!isListLoading && !listError && items.length === 0 && (
            <p className="spending-income-list-empty">
              아직 등록한 내역이 없어요.
            </p>
          )}
          {!isListLoading && !listError && items.length > 0 && (
            <ul>
              {items.map((item) => (
                <li key={item.id}>
                  <span
                    className={
                      Number(item.amount) < 0
                        ? "spending-income-amount negative"
                        : "spending-income-amount"
                    }
                  >
                    {Number(item.amount) > 0 ? "+" : ""}
                    {WON.format(Number(item.amount))}원
                  </span>
                  <span className="spending-income-memo">
                    {item.memo || "메모 없음"} · {formatDate(item.occurredAt)}
                  </span>
                  <button
                    type="button"
                    className="spending-income-delete"
                    onClick={() => handleDelete(item.id)}
                    disabled={deletingId === item.id}
                    aria-label="삭제"
                  >
                    <DashboardIcon name="x" size={14} />
                  </button>
                </li>
              ))}
            </ul>
          )}
        </div>
      </section>
    </div>
  );
}

export default IncomeAdjustmentModal;
