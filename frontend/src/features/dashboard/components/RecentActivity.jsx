import { Link } from "react-router-dom";
import DashboardIcon from "../../../components/common/DashboardIcon";
import DashEmptyState from "./DashEmptyState";
import DashSectionError from "./DashSectionError";
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
      {!loading && items.length === 0 && (
        <DashEmptyState
          icon="receipt"
          title="최근 소비 내역이 없어요"
          description="소비가이드를 설정하고 소비가 발생하면 여기에 자동으로 표시돼요."
        />
      )}
      {items.length > 0 && (
        <>
          {error && <DashSectionError message={error} />}
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
