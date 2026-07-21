import { useState } from "react";
import DashboardIcon from "../../../components/common/DashboardIcon";
import { disconnectCardConnection } from "../../../api/spendingGuideApi";
import { getApiErrorMessage } from "../../../utils/apiError";

function DisconnectCardModal({ connection, onClose, onDisconnected }) {
  const [error, setError] = useState("");
  const [isSubmitting, setIsSubmitting] = useState(false);

  const handleConfirm = async () => {
    setError("");
    setIsSubmitting(true);
    try {
      await disconnectCardConnection(connection.connectionId);
      await onDisconnected();
    } catch (requestError) {
      setError(
        getApiErrorMessage(requestError, "카드 연동을 해제하지 못했어요."),
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
        aria-labelledby="mp-disconnect-title"
        onMouseDown={(event) => event.stopPropagation()}
      >
        <div className="mp-modal-head">
          <div>
            <h2 id="mp-disconnect-title">
              {connection.organizationName} 연동 해제
            </h2>
            <p>연동을 해제하기 전에 아래 내용을 확인해 주세요.</p>
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
        <div className="mp-modal-body">
          <div className="mp-modal-warning">
            <DashboardIcon name="info" size={16} />
            <span>
              연동을 해제하면 이 카드사의 결제내역이 더 이상 자동으로
              동기화되지 않아요. 이미 수집된 내역은 그대로 남아 있고, 언제든
              다시 연결할 수 있어요.
            </span>
          </div>
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
              type="button"
              className="mp-danger-button"
              onClick={handleConfirm}
              disabled={isSubmitting}
            >
              {isSubmitting ? "해제 중..." : "연동 해제"}
            </button>
          </div>
        </div>
      </section>
    </div>
  );
}

export default DisconnectCardModal;
