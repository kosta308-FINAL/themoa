import { Link } from "react-router-dom";
import DashboardIcon from "../../../components/common/DashboardIcon";
import { formatPolicyPeriod } from "../dashboardUtils";

function PolicyRecommendations({ bookmarks, loading, error }) {
  const items = (bookmarks?.items || []).slice(0, 3);

  return (
    <div className="widget-panel">
      <div className="widget-panel-header">
        <h3>관심 정책</h3>
        <Link to="/dashboard/policy">더보기 &gt;</Link>
      </div>
      {loading && !bookmarks && <div className="dash-loading">관심 정책을 불러오고 있어요.</div>}
      {error && !bookmarks && <div className="dash-section-error">관심 정책을 불러오지 못했어요.</div>}
      {!loading && !error && items.length === 0 && (
        <div className="dash-empty-state">아직 관심 정책이 없어요.</div>
      )}
      {items.length > 0 && (
        <>
          {error && <div className="dash-section-error">{error}</div>}
          <ul className="policy-list">
            {items.map((policy) => (
              <li key={policy.bookmarkId || policy.policyId}>
                <span className="policy-icon">
                  <DashboardIcon name="building" />
                </span>
                <div className="policy-info">
                  <strong>{policy.title}</strong>
                  <span>{policy.agencyName} · {policy.category}</span>
                  <span>{formatPolicyPeriod(policy)}</span>
                </div>
                <span className="policy-status">
                  {policy.active === false ? "비활성" : policy.policyStatus}
                </span>
              </li>
            ))}
          </ul>
        </>
      )}
    </div>
  );
}

export default PolicyRecommendations;
