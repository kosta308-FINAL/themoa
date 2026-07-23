import { Link } from "react-router-dom";
import DashboardIcon from "../../../components/common/DashboardIcon";
import { formatDateTime, formatTransactionAmount } from "../dashboardUtils";

function RecentActivity({ transactions, loading, error }) {
  const items = (transactions?.items || []).slice(0, 5);

  return (
    <div className="widget-panel">
      <div className="widget-panel-header">
        <h3>최근 소비</h3>
        <Link to="/dashboard/spending/transactions">더보기 &gt;</Link>
      </div>
      {loading && !transactions && <div className="dash-loading">최근 소비를 불러오고 있어요.</div>}
      {error && !transactions && <div className="dash-section-error">최근 소비 내역을 불러오지 못했어요.</div>}
      {!loading && !error && items.length === 0 && (
        <div className="dash-empty-state">최근 소비 내역이 없어요.</div>
      )}
      {items.length > 0 && (
        <>
          {error && <div className="dash-section-error">{error}</div>}
          <ul className="activity-list">
            {items.map((item) => (
              <li key={item.transactionId || `${item.usedAt}-${item.netAmount}`}>
                <span className="activity-icon">
                  <DashboardIcon name="card" />
                </span>
                <div className="activity-info">
                  <strong>{item.merchantDisplayName || item.merchantNameRaw || "가맹점 정보 없음"}</strong>
                  <span>{item.categoryName || "카테고리 없음"}</span>
                </div>
                <div className="activity-amount-col">
                  <span className={item.netAmount > 0 ? "activity-amount-negative" : "activity-amount-positive"}>
                    {formatTransactionAmount(item.netAmount)}
                  </span>
                  <span className="activity-time">{formatDateTime(item.usedAt || item.usedDate) || item.usedDate || ""}</span>
                </div>
              </li>
            ))}
          </ul>
        </>
      )}
    </div>
  );
}

export default RecentActivity;
