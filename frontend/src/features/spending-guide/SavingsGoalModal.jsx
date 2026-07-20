import { useState } from "react";
import { updateSavingsGoal } from "../../api/spendingGuideApi";
import DashboardIcon from "../../components/common/DashboardIcon";
import { errorMessage, WON } from "./spendingGuideUtils";

const digits = (value) => value.replace(/\D/g, "").slice(0, 12);

function SavingsGoalModal({ currentAmount, onClose, onSaved }) {
  const [amount, setAmount] = useState(String(Number(currentAmount || 0)));
  const [applyFrom, setApplyFrom] = useState("CURRENT_CYCLE");
  const [error, setError] = useState("");
  const [isSubmitting, setIsSubmitting] = useState(false);

  const handleSubmit = async (event) => {
    event.preventDefault();
    setError("");
    setIsSubmitting(true);
    try {
      await updateSavingsGoal({ amount: Number(amount || 0), applyFrom });
      await onSaved();
      onClose();
    } catch (requestError) {
      setError(errorMessage(requestError, "저축 목표를 저장하지 못했습니다."));
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <div
      className="spending-modal-backdrop"
      role="presentation"
      onMouseDown={onClose}
    >
      <section
        className="spending-modal spending-savings-modal"
        role="dialog"
        aria-modal="true"
        aria-labelledby="savings-goal-title"
        onMouseDown={(event) => event.stopPropagation()}
      >
        <div className="spending-modal-head">
          <div>
            <h2 id="savings-goal-title">월 저축 목표</h2>
            <p>
              매달 얼마를 남기고 싶은지 정하면 잉여금을 목표에 맞게
              관리해드려요.
            </p>
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
            <span>월 저축 목표</span>
            <div className="spending-input-suffix">
              <input
                inputMode="numeric"
                value={amount ? WON.format(Number(amount)) : ""}
                onChange={(event) => setAmount(digits(event.target.value))}
                placeholder="0"
              />
              <em>원</em>
            </div>
          </label>
          <label className="wide">
            <span>적용 시점 *</span>
            <select
              value={applyFrom}
              onChange={(event) => setApplyFrom(event.target.value)}
            >
              <option value="CURRENT_CYCLE">이번 급여 주기부터</option>
              <option value="NEXT_CYCLE">다음 급여 주기부터</option>
            </select>
          </label>
          <div className="spending-form-notice wide">
            <DashboardIcon name="info" size={17} />
            <span>0원으로 두면 저축 목표 없이 잉여금을 자유롭게 사용해요.</span>
          </div>
          {error && (
            <div className="spending-form-error wide">
              <DashboardIcon name="info" size={16} />
              {error}
            </div>
          )}
          <button
            type="submit"
            className="spending-primary wide"
            disabled={isSubmitting}
          >
            {isSubmitting ? "저장 중..." : "저축 목표 저장"}
          </button>
        </form>
      </section>
    </div>
  );
}

export default SavingsGoalModal;
