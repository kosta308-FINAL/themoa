import DashboardIcon from '../../../components/common/DashboardIcon'
import { policyRecommendations } from '../../../constants/mockDashboard'

function PolicyRecommendations() {
  return (
    <div className="widget-panel">
      <div className="widget-panel-header">
        <h3>정부지원정책 추천</h3>
        <a href="#">더보기 &gt;</a>
      </div>
      <ul className="policy-list">
        {policyRecommendations.map((policy) => (
          <li key={policy.title}>
            <span className="policy-icon">
              <DashboardIcon name={policy.icon} />
            </span>
            <div className="policy-info">
              <strong>{policy.title}</strong>
              <span>{policy.detail}</span>
            </div>
            <span className="policy-status">{policy.status}</span>
          </li>
        ))}
      </ul>
    </div>
  )
}

export default PolicyRecommendations
