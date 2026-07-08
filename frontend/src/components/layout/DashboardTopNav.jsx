import { NavLink } from 'react-router-dom'
import DashboardIcon from '../common/DashboardIcon'
import { navItems } from '../../constants/mockDashboard'

function DashboardTopNav() {
  return (
    <nav className="dash-topnav">
      <div className="dash-topnav-logo">
        <span className="dash-logo-mark">M</span>
        <span className="dash-logo-text">더모아</span>
      </div>
      <div className="dash-topnav-right">
        <div className="dash-topnav-search">
          <DashboardIcon name="search" size={16} />
          <input type="text" placeholder="검색어를 입력하세요" />
        </div>
        <div className="dash-topnav-items">
          {navItems.map((item, idx) => (
            <NavLink
              key={item.key}
              to={idx === 0 ? '/dashboard' : `/dashboard/${item.key}`}
              className={({ isActive }) => `dash-nav-item${isActive ? ' active' : ''}`}
              end={idx === 0}
            >
              <DashboardIcon name={item.icon} />
              <span>{item.label}</span>
            </NavLink>
          ))}
        </div>
      </div>
    </nav>
  )
}

export default DashboardTopNav
