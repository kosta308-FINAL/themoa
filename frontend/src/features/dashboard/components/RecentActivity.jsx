import DashboardIcon from '../../../components/common/DashboardIcon'
import { recentActivity } from '../../../constants/mockDashboard'

function RecentActivity() {
  return (
    <div className="widget-panel">
      <div className="widget-panel-header">
        <h3>최근 활동</h3>
        <a href="#">더보기 &gt;</a>
      </div>
      <ul className="activity-list">
        {recentActivity.map((item) => (
          <li key={item.title}>
            <span className="activity-icon">
              <DashboardIcon name={item.icon} />
            </span>
            <div className="activity-info">
              <strong>{item.title}</strong>
              <span>{item.category}</span>
            </div>
            <div className="activity-amount-col">
              <span className={item.negative ? 'activity-amount-negative' : 'activity-amount-positive'}>
                {item.amount}
              </span>
              <span className="activity-time">{item.time}</span>
            </div>
          </li>
        ))}
      </ul>
    </div>
  )
}

export default RecentActivity
