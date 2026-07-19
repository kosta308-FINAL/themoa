import DashboardTopbar from "./components/DashboardTopbar";
import SummaryCards from "./components/SummaryCards";
import AiChatCard from "./components/AiChatCard";
import StatsStrip from "./components/StatsStrip";
import SpendingAnalysis from "./components/SpendingAnalysis";
import RecommendedProduct from "./components/RecommendedProduct";
import PolicyRecommendations from "./components/PolicyRecommendations";
import RecentActivity from "./components/RecentActivity";
import SpendingTipCard from "./components/SpendingTipCard";
import InviteFriendCard from "./components/InviteFriendCard";
import "./Dashboard.css";

function Dashboard() {
  return (
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
  );
}

export default Dashboard;
