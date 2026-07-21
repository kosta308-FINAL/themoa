import { useState } from "react";
import DashboardIcon from "../../../components/common/DashboardIcon";
import { CONNECTION_STATUS_LABELS, formatDateTime } from "../mypageUtils";
import DisconnectCardModal from "./DisconnectCardModal";
import AddCardConnectionModal from "./AddCardConnectionModal";

function CardConnectionCard({ cardConnections, onChanged }) {
  const connections = cardConnections?.connections || [];
  const [disconnectTarget, setDisconnectTarget] = useState(null);
  const [isAdding, setIsAdding] = useState(false);

  const handleDisconnected = async () => {
    setDisconnectTarget(null);
    await onChanged();
  };

  const handleConnected = async () => {
    setIsAdding(false);
    await onChanged();
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
      ) : connections.length === 0 ? (
        <p className="mp-empty">아직 연결된 카드사가 없어요.</p>
      ) : (
        <>
          <p className="mp-card-sub">
            자동수집 {cardConnections.cardSyncEnabled ? "ON" : "OFF"} · 자동수집
            설정은 소비가이드에서 변경할 수 있어요.
          </p>
          <ul className="mp-connection-list">
            {connections.map((connection) => (
              <li key={connection.connectionId}>
                <div className="mp-connection-row">
                  <div className="mp-connection-info">
                    <strong>{connection.organizationName}</strong>
                    <span
                      className={`mp-status-badge mp-status-${connection.connectionStatus?.toLowerCase()}`}
                    >
                      {CONNECTION_STATUS_LABELS[connection.connectionStatus] ||
                        connection.connectionStatus}
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
