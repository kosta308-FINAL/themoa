import { formatDateTime } from "../dashboardUtils";

function DashboardTopbar({
  name,
  lastUpdatedAt,
  isRefreshing,
  onRefresh,
}) {
  return (
    <div className="dash-topbar">
      <div>
        <h1>{name ? `안녕하세요, ${name}님` : "안녕하세요"}</h1>
        <p>오늘도 똑똑한 금융 습관으로 한 걸음 더 나아가요.</p>
      </div>
      <div className="dash-topbar-actions">
        {lastUpdatedAt && (
          <span className="dash-updated">
            최근 업데이트: {formatDateTime(lastUpdatedAt)} 기준
          </span>
        )}
        <button
          type="button"
          className="dash-refresh-button"
          disabled={isRefreshing}
          onClick={onRefresh}
        >
          {isRefreshing ? "새로고침 중..." : "새로고침"}
        </button>
      </div>
    </div>
  );
}

export default DashboardTopbar;
