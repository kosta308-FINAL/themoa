import DashboardTopNav from '../../components/dashboard/DashboardTopNav'
import DashboardFooter from '../../components/dashboard/DashboardFooter'
import '../Dashboard/Dashboard.css'

function SectionStub({ title, description }) {
  return (
    <div className="dashboard">
      <DashboardTopNav />
      <main className="dash-main">
        <div className="dash-topbar">
          <div>
            <h1>{title}</h1>
            <p>{description}</p>
          </div>
        </div>
        <div className="dash-placeholder">준비 중인 페이지입니다.</div>
      </main>
      <DashboardFooter />
    </div>
  )
}

export default SectionStub
