import DashboardTopNav from '../../components/dashboard/DashboardTopNav'
import DashboardTopbar from '../../components/dashboard/DashboardTopbar'
import SummaryCards from '../../components/dashboard/SummaryCards'
import AiChatCard from '../../components/dashboard/AiChatCard'
import StatsStrip from '../../components/dashboard/StatsStrip'
import SpendingAnalysis from '../../components/dashboard/SpendingAnalysis'
import RecommendedProduct from '../../components/dashboard/RecommendedProduct'
import PolicyRecommendations from '../../components/dashboard/PolicyRecommendations'
import RecentActivity from '../../components/dashboard/RecentActivity'
import SpendingTipCard from '../../components/dashboard/SpendingTipCard'
import InviteFriendCard from '../../components/dashboard/InviteFriendCard'
import DashboardFooter from '../../components/dashboard/DashboardFooter'
import './Dashboard.css'

function Dashboard() {
  return (
    <div className="dashboard">
      <DashboardTopNav />
      <main className="dash-main">
        <section className="dash-hero-section dash-hero-section-split">
          <div className="dash-hero-card">
            <DashboardTopbar />
            <SummaryCards />
          </div>
          <div className="dash-hero-side">
            <AiChatCard />
          </div>
        </section>
        <StatsStrip />
        <div className="dash-widget-row">
          <SpendingAnalysis />
          <RecommendedProduct />
          <PolicyRecommendations />
        </div>
        <div className="dash-widget-row">
          <RecentActivity />
          <SpendingTipCard />
          <InviteFriendCard />
        </div>
      </main>
      <DashboardFooter />
    </div>
  )
}

export default Dashboard
