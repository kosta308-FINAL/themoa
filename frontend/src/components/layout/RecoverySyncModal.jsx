import { useState } from "react";
import { recoverSync } from "../../api/spendingGuideApi";
import { getApiErrorMessage } from "../../utils/apiError";
import DashboardIcon from "../common/DashboardIcon";

const MODE_LABEL = {
  CURRENT_MONTH: "이번 달부터 시작",
  RECOVER_RECENT: "최근 내역도 불러오기",
};

function RecoverySyncModal({ onClose }) {
  const [pendingMode, setPendingMode] = useState("");
  const [result, setResult] = useState(null);
  const [resultLabel, setResultLabel] = useState("");
  const [error, setError] = useState("");

  const handleSelect = async (mode) => {
    setError("");
    setPendingMode(mode);
    try {
      const response = await recoverSync(mode);
      if (response?.locked) {
        setError(
          "이미 다른 동기화가 진행 중이에요. 잠시 후 다시 시도해 주세요.",
        );
        return;
      }
      setResult(response);
      setResultLabel(MODE_LABEL[mode]);
    } catch (requestError) {
      setError(getApiErrorMessage(requestError, "복귀 동기화에 실패했어요."));
    } finally {
      setPendingMode("");
    }
  };

  return (
    <div className="dash-recovery-backdrop" role="presentation">
      <section
        className="dash-recovery-modal"
        role="dialog"
        aria-modal="true"
        aria-labelledby="recovery-modal-title"
      >
        <button
          type="button"
          className="dash-recovery-close"
          onClick={onClose}
          aria-label="닫기"
        >
          <DashboardIcon name="x" size={16} />
        </button>
        {result ? (
          <div className="dash-recovery-result">
            <DashboardIcon name="check" size={28} />
            <h2>{resultLabel} 완료</h2>
            <p>
              새로 {result.created}건, 갱신 {result.updated}건을 반영했어요.
            </p>
            <button
              type="button"
              className="dash-recovery-primary"
              onClick={onClose}
            >
              확인
            </button>
          </div>
        ) : (
          <>
            <h2 id="recovery-modal-title">오랜만이에요!</h2>
            <p>그동안 놓친 카드 내역을 어디서부터 불러올지 선택해 주세요.</p>
            {error && <div className="dash-recovery-error">{error}</div>}
            <div className="dash-recovery-options">
              <button
                type="button"
                className="dash-recovery-primary"
                disabled={Boolean(pendingMode)}
                onClick={() => handleSelect("CURRENT_MONTH")}
              >
                {pendingMode === "CURRENT_MONTH"
                  ? "불러오는 중..."
                  : "이번 달부터 시작"}
              </button>
              <button
                type="button"
                className="dash-recovery-secondary"
                disabled={Boolean(pendingMode)}
                onClick={() => handleSelect("RECOVER_RECENT")}
              >
                {pendingMode === "RECOVER_RECENT"
                  ? "불러오는 중..."
                  : "최근 내역도 불러오기"}
              </button>
            </div>
            {pendingMode && (
              <p className="dash-recovery-hint">
                카드사 내역을 조회하고 있어요. 최대 몇 분 정도 걸릴 수
                있어요.
              </p>
            )}
          </>
        )}
      </section>
    </div>
  );
}

export default RecoverySyncModal;
