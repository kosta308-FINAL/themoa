import { useState } from "react";
import DashboardIcon from "../../../components/common/DashboardIcon";
import { withdrawAccount } from "../../../api/authApi";
import { getApiErrorMessage } from "../../../utils/apiError";

function WithdrawAccountModal({ onClose, onWithdrawn }) {
  const [password, setPassword] = useState("");
  const [error, setError] = useState("");
  const [isSubmitting, setIsSubmitting] = useState(false);

  const handleSubmit = async (event) => {
    event.preventDefault();
    setError("");
    setIsSubmitting(true);
    try {
      await withdrawAccount({ password });
      await onWithdrawn();
    } catch (requestError) {
      setError(
        getApiErrorMessage(requestError, "회원 탈퇴를 처리하지 못했어요."),
      );
      setIsSubmitting(false);
    }
  };

  return (
    <div
      className="mp-modal-backdrop"
      role="presentation"
      onMouseDown={onClose}
    >
      <section
        className="mp-modal mp-modal-sm"
        role="dialog"
        aria-modal="true"
        aria-labelledby="mp-withdraw-title"
        onMouseDown={(event) => event.stopPropagation()}
      >
        <div className="mp-modal-head">
          <div>
            <h2 id="mp-withdraw-title">회원 탈퇴</h2>
            <p>탈퇴 전에 아래 내용을 확인해 주세요.</p>
          </div>
          <button
            type="button"
            className="mp-modal-close"
            onClick={onClose}
            aria-label="닫기"
          >
            ×
          </button>
        </div>
        <form className="mp-modal-body mp-inline-form" onSubmit={handleSubmit}>
          <div className="mp-modal-warning mp-modal-warning-danger">
            <DashboardIcon name="info" size={16} />
            <span>
              탈퇴하면 즉시 로그아웃되고 이 계정으로 다시 로그인할 수 없어요.
              카드 연동·소비 내역 등 회원님의 데이터는 관련 법령에 따라 지체
              없이 파기됩니다. 이 작업은 되돌릴 수 없어요.
            </span>
          </div>
          <label>
            <span>비밀번호 확인 *</span>
            <input
              type="password"
              value={password}
              onChange={(event) => setPassword(event.target.value)}
              autoComplete="current-password"
              required
            />
          </label>
          {error && (
            <div className="mp-form-error">
              <DashboardIcon name="info" size={16} />
              {error}
            </div>
          )}
          <div className="mp-inline-form-actions">
            <button
              type="button"
              className="mp-ghost-button"
              onClick={onClose}
              disabled={isSubmitting}
            >
              취소
            </button>
            <button
              type="submit"
              className="mp-danger-button"
              disabled={isSubmitting}
            >
              {isSubmitting ? "탈퇴 처리 중..." : "회원 탈퇴"}
            </button>
          </div>
        </form>
      </section>
    </div>
  );
}

export default WithdrawAccountModal;
