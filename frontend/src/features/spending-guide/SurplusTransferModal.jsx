import { useState } from "react";
import { createSurplusTransfer } from "../../api/spendingGuideApi";
import DashboardIcon from "../../components/common/DashboardIcon";
import {
  errorMessage,
  formatWon,
  toNumber,
  WON,
} from "./spendingGuideUtils";

const digits = (value) => value.replace(/\D/g, "").slice(0, 12);

function SurplusTransferModal({
  availableAmount,
  currentRemainingAmount,
  onClose,
  onSaved,
}) {
  const [amount, setAmount] = useState("");
  const [error, setError] = useState("");
  const [isSubmitting, setIsSubmitting] = useState(false);
  const numericAmount = toNumber(amount);
  const available = toNumber(availableAmount);
  const exceedsAvailable = numericAmount > available;

  const handleSubmit = async (event) => {
    event.preventDefault();
    if (numericAmount <= 0 || exceedsAvailable) return;

    setError("");
    setIsSubmitting(true);
    try {
      await createSurplusTransfer({ amount: numericAmount });
      await onSaved();
      onClose();
    } catch (requestError) {
      setError(
        errorMessage(requestError, "잉여금을 이번 주기로 가져오지 못했습니다."),
      );
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
        className="spending-modal spending-surplus-transfer-modal"
        role="dialog"
        aria-modal="true"
        aria-labelledby="surplus-transfer-title"
        onMouseDown={(event) => event.stopPropagation()}
      >
        <div className="spending-modal-head">
          <div>
            <h2 id="surplus-transfer-title">이번 주기 예산으로 가져오기</h2>
            <p>완료된 주기에서 남긴 잉여금 일부를 현재 예산에 더해요.</p>
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
          <div className="spending-surplus-transfer-balance wide">
            <span>사용 가능한 잉여금</span>
            <strong>{formatWon(available)}</strong>
          </div>

          <label className="wide">
            <span>가져올 금액 *</span>
            <div className="spending-input-suffix">
              <input
                inputMode="numeric"
                value={amount ? WON.format(numericAmount) : ""}
                onChange={(event) => {
                  setAmount(digits(event.target.value));
                  setError("");
                }}
                placeholder="0"
                required
                autoFocus
              />
              <em>원</em>
            </div>
          </label>

          {numericAmount > 0 && !exceedsAvailable && (
            <div className="spending-surplus-transfer-preview wide">
              <span>적용 후 이번 주기 남은 예산</span>
              <strong>
                {formatWon(toNumber(currentRemainingAmount) + numericAmount)}
              </strong>
            </div>
          )}

          {exceedsAvailable && (
            <div className="spending-form-error wide">
              <DashboardIcon name="info" size={16} />
              사용 가능한 잉여금 안에서 입력해주세요.
            </div>
          )}
          {error && (
            <div className="spending-form-error wide">
              <DashboardIcon name="info" size={16} />
              {error}
            </div>
          )}

          <p className="spending-surplus-transfer-note wide">
            가져온 금액은 예산만 이동하며 실제 계좌 이체는 일어나지 않아요.
            금융상품 추천의 월 납입가능금액 평균에서도 즉시 제외됩니다.
          </p>

          <button
            type="submit"
            className="spending-primary wide"
            disabled={isSubmitting || numericAmount <= 0 || exceedsAvailable}
          >
            {isSubmitting ? "가져오는 중..." : "이번 주기에 가져오기"}
          </button>
        </form>
      </section>
    </div>
  );
}

export default SurplusTransferModal;
