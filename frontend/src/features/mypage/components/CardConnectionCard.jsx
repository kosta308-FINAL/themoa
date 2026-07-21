import { Link } from "react-router-dom";
import DashboardIcon from "../../../components/common/DashboardIcon";
import { CONNECTION_STATUS_LABELS, formatDateTime } from "../mypageUtils";

function CardConnectionCard({ cardConnections }) {
  const connections = cardConnections?.connections || [];

  return (
    <section className="mp-card">
      <div className="mp-card-head">
        <span className="mp-card-icon">
          <DashboardIcon name="card" size={17} />
        </span>
        <h2>카드 연동 현황</h2>
      </div>

      {!cardConnections ? (
        <p className="mp-empty">카드 연동 정보를 불러오지 못했어요.</p>
      ) : connections.length === 0 ? (
        <p className="mp-empty">아직 연결된 카드사가 없어요.</p>
      ) : (
        <>
          <p className="mp-card-sub">
            자동수집 {cardConnections.cardSyncEnabled ? "ON" : "OFF"}
          </p>
          <ul className="mp-connection-list">
            {connections.map((connection) => (
              <li key={connection.connectionId}>
                <strong>{connection.organizationName}</strong>
                <span
                  className={`mp-status-badge mp-status-${connection.connectionStatus?.toLowerCase()}`}
                >
                  {CONNECTION_STATUS_LABELS[connection.connectionStatus] ||
                    connection.connectionStatus}
                </span>
                <p>
                  마지막 동기화{" "}
                  {formatDateTime(connection.lastSuccessfulSyncAt)}
                </p>
              </li>
            ))}
          </ul>
        </>
      )}

      <Link to="/dashboard/spending" className="mp-ghost-button">
        소비가이드에서 카드 관리하기
        <DashboardIcon name="chevron-right" size={14} />
      </Link>
    </section>
  );
}

export default CardConnectionCard;
