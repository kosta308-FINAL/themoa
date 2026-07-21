import { useState } from "react";
import DashboardIcon from "../../../components/common/DashboardIcon";
import { updateCardSyncEnabled } from "../../../api/spendingGuideApi";
import { getApiErrorMessage } from "../../../utils/apiError";
import { CONNECTION_STATUS_LABELS, formatDateTime } from "../mypageUtils";
import DisconnectCardModal from "./DisconnectCardModal";
import AddCardConnectionModal from "./AddCardConnectionModal";

function CardConnectionCard({ cardConnections, onChanged }) {
  const connections = cardConnections?.connections || [];
  const [disconnectTarget, setDisconnectTarget] = useState(null);
  const [isAdding, setIsAdding] = useState(false);
  const [isTogglingSync, setIsTogglingSync] = useState(false);
  const [toggleError, setToggleError] = useState("");

  const handleDisconnected = async () => {
    setDisconnectTarget(null);
    await onChanged();
  };

  const handleConnected = async () => {
    setIsAdding(false);
    await onChanged();
  };

  const handleToggleSync = async () => {
    setToggleError("");
    setIsTogglingSync(true);
    try {
      await updateCardSyncEnabled(!cardConnections.cardSyncEnabled);
      await onChanged();
    } catch (requestError) {
      setToggleError(
        getApiErrorMessage(requestError, "자동수집 설정을 변경하지 못했어요."),
      );
    } finally {
      setIsTogglingSync(false);
    }
  };

  return (
    <section className="mp-card">
      <div className="mp-card-head">
        <span className="mp-card-icon">
          <DashboardIcon name="card" size={17} />
        </span>
        <h2>카드 연동 현황</h2>
        <button
          type="button"
          className="mp-ghost-button mp-card-head-action"
          onClick={() => setIsAdding(true)}
        >
          <DashboardIcon name="plus" size={14} />
          카드사 추가
        </button>
      </div>

      {!cardConnections ? (
        <p className="mp-empty">카드 연동 정보를 불러오지 못했어요.</p>
      ) : (
        <>
          <div className="mp-sync-toggle-row">
            <div>
              <strong>카드 자동수집</strong>
              <p>연결과 기존 거래는 설정을 꺼도 그대로 유지돼요.</p>
            </div>
            <button
              type="button"
              className={
                cardConnections.cardSyncEnabled ? "mp-switch on" : "mp-switch"
              }
              onClick={handleToggleSync}
              disabled={isTogglingSync}
              aria-label={
                cardConnections.cardSyncEnabled
                  ? "자동수집 끄기"
                  : "자동수집 켜기"
              }
            >
              <i />
            </button>
          </div>
          {toggleError && (
            <div className="mp-form-error">
              <DashboardIcon name="info" size={16} />
              {toggleError}
            </div>
          )}
          {connections.length === 0 ? (
            <p className="mp-empty">아직 연결된 카드사가 없어요.</p>
          ) : (
            <ul className="mp-connection-list">
              {connections.map((connection) => (
                <li key={connection.connectionId}>
                  <div className="mp-connection-row">
                    <div className="mp-connection-info">
                      <strong>{connection.organizationName}</strong>
                      <span
                        className={`mp-status-badge mp-status-${connection.connectionStatus?.toLowerCase()}`}
                      >
                        {CONNECTION_STATUS_LABELS[
                          connection.connectionStatus
                        ] || connection.connectionStatus}
                      </span>
                    </div>
                    <button
                      type="button"
                      className="mp-text-danger-button"
                      onClick={() => setDisconnectTarget(connection)}
                    >
                      연동 해제
                    </button>
                  </div>
                  <p>
                    마지막 동기화{" "}
                    {formatDateTime(connection.lastSuccessfulSyncAt)}
                  </p>
                </li>
              ))}
            </ul>
          )}
        </>
      )}

      {disconnectTarget && (
        <DisconnectCardModal
          connection={disconnectTarget}
          onClose={() => setDisconnectTarget(null)}
          onDisconnected={handleDisconnected}
        />
      )}

      {isAdding && (
        <AddCardConnectionModal
          onClose={() => setIsAdding(false)}
          onConnected={handleConnected}
        />
      )}
    </section>
  );
}

export default CardConnectionCard;
