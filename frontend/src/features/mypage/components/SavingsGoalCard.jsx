import { useState } from "react";
import DashboardIcon from "../../../components/common/DashboardIcon";
import { updateSavingsGoal } from "../../../api/spendingGuideApi";
import { getApiErrorMessage } from "../../../utils/apiError";
import { formatWon } from "../mypageUtils";

const digits = (value) => value.replace(/\D/g, "").slice(0, 12);

function SavingsGoalCard({ savingsTargetAmount, onSaved }) {
  const [isEditing, setIsEditing] = useState(false);
  const [amount, setAmount] = useState(String(savingsTargetAmount || 0));
  const [applyFrom, setApplyFrom] = useState("CURRENT_CYCLE");
  const [error, setError] = useState("");
  const [isSubmitting, setIsSubmitting] = useState(false);

  const startEdit = () => {
    setAmount(String(savingsTargetAmount || 0));
    setApplyFrom("CURRENT_CYCLE");
    setError("");
    setIsEditing(true);
  };

  const handleSubmit = async (event) => {
    event.preventDefault();
    setError("");
    setIsSubmitting(true);
    try {
      await updateSavingsGoal({ amount: Number(amount || 0), applyFrom });
      setIsEditing(false);
      await onSaved("월 저축목표를 저장했어요.");
    } catch (requestError) {
      setError(
        getApiErrorMessage(requestError, "저축목표를 저장하지 못했어요."),
      );
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <section className="mp-card">
      <div className="mp-card-head">
        <span className="mp-card-icon">
          <DashboardIcon name="target" size={17} />
        </span>
        <h2>월 저축목표</h2>
      </div>

      {!isEditing ? (
        <>
          <p className="mp-savings-amount">{formatWon(savingsTargetAmount)}</p>
          <button type="button" className="mp-ghost-button" onClick={startEdit}>
            <DashboardIcon name="edit" size={14} />
            목표 수정
          </button>
        </>
      ) : (
        <form className="mp-inline-form" onSubmit={handleSubmit}>
          <label>
            <span>목표 금액</span>
            <div className="mp-input-suffix">
              <input
                inputMode="numeric"
                value={amount ? Number(amount).toLocaleString("ko-KR") : ""}
                onChange={(event) => setAmount(digits(event.target.value))}
                placeholder="0"
              />
              <em>원</em>
            </div>
          </label>
          <label>
            <span>적용 시점</span>
            <select
              value={applyFrom}
              onChange={(event) => setApplyFrom(event.target.value)}
            >
              <option value="CURRENT_CYCLE">이번 급여 주기부터</option>
              <option value="NEXT_CYCLE">다음 급여 주기부터</option>
            </select>
          </label>
          {error && <p className="mp-form-error">{error}</p>}
          <div className="mp-inline-form-actions">
            <button
              type="button"
              className="mp-ghost-button"
              onClick={() => setIsEditing(false)}
              disabled={isSubmitting}
            >
              취소
            </button>
            <button
              type="submit"
              className="mp-primary-button"
              disabled={isSubmitting}
            >
              {isSubmitting ? "저장 중..." : "저장"}
            </button>
          </div>
        </form>
      )}
    </section>
  );
}

export default SavingsGoalCard;
