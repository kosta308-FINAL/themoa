import './DashboardHeader.css'

function DashboardHeader({ tabs, activeTab, onChange }) {
  return (
    <header className="dash-header">
      <div className="dash-header-inner">
        <button type="button" className="dash-logo" onClick={() => onChange('dashboard')}>
          <div className="dash-logo-mark">
            <svg width="18" height="18" viewBox="0 0 20 20" fill="none">
              <path d="M10 2L2 7v6l8 5 8-5V7L10 2z" fill="var(--accent)" opacity="0.15" />
              <path d="M10 2L2 7l8 5 8-5-8-5z" fill="var(--accent)" />
              <path d="M2 7v6l8 5V12L2 7z" fill="var(--accent)" opacity="0.7" />
              <path d="M18 7v6l-8 5V12l8-5z" fill="var(--accent)" opacity="0.5" />
            </svg>
          </div>
          <span className="dash-logo-text">Themore</span>
        </button>

        <nav className="dash-nav">
          {tabs.map((tab) => (
            <button
              key={tab.id}
              type="button"
              className={`dash-nav-link${activeTab === tab.id ? ' active' : ''}`}
              onClick={() => onChange(tab.id)}
            >
              {tab.label}
            </button>
          ))}
        </nav>

        <div className="dash-header-actions">
          <div className="dash-avatar">솔</div>
        </div>
      </div>
    </header>
  )
}

export default DashboardHeader
