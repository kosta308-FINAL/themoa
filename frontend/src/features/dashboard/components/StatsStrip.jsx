import DashboardIcon from '../../../components/common/DashboardIcon'
import { statsStrip } from '../../../constants/mockDashboard'

function StatsStrip() {
  return (
    <div className="stats-strip">
      {statsStrip.map((stat) => (
        <div className="stats-item" key={stat.label}>
          <span className="stats-icon">
            <DashboardIcon name={stat.icon} />
          </span>
          <div>
            <span className="stats-label">{stat.label}</span>
            <p>
              {stat.value}
              <span className="stats-unit">{stat.unit}</span>
            </p>
          </div>
        </div>
      ))}
    </div>
  )
}

export default StatsStrip
