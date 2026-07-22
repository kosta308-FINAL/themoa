import DashboardTopbar from "./components/DashboardTopbar";
import SummaryCards from "./components/SummaryCards";
import StatsStrip from "./components/StatsStrip";
import SpendingAnalysis from "./components/SpendingAnalysis";
import RecommendedProduct from "./components/RecommendedProduct";
import PolicyRecommendations from "./components/PolicyRecommendations";
import RecentActivity from "./components/RecentActivity";
import SpendingTipCard from "./components/SpendingTipCard";
import DashboardWeeklyCalendar from "./components/DashboardWeeklyCalendar";
import { useDashboardData } from "./hooks/useDashboardData";
import "./Dashboard.css";

function Dashboard() {
  const {
    data,
    sectionErrors,
    isLoading,
    isRefreshing,
    lastUpdatedAt,
    reload,
  } = useDashboardData();
  const productBookmarks = data.productBookmarks?.items || data.productBookmarks || [];
  const policyBookmarks = data.policyBookmarks?.items || [];
  const coachingItems = data.coaching?.items || [];

  return (
    <main className="dash-main">
      <section className="dash-hero-section dash-hero-section-split">
        <div className="dash-hero-card">
          <DashboardTopbar
            name={data.myPage?.profile?.name}
            lastUpdatedAt={lastUpdatedAt}
            isRefreshing={isRefreshing}
            onRefresh={reload}
          />
          <SummaryCards
            summary={data.summary}
            productBookmarkCount={productBookmarks.length}
            policyBookmarkCount={policyBookmarks.length}
            coachingCount={coachingItems.length}
            loading={isLoading}
            error={sectionErrors.summary}
          />
        </div>
        <div className="dash-hero-side">
          <DashboardWeeklyCalendar />
        </div>
      </section>
      <StatsStrip
        summary={data.summary}
        todayTransactionCount={data.today?.totalCount}
        loading={isLoading}
        error={sectionErrors.summary || sectionErrors.today}
      />
      <section className="dash-primary-grid">
        <SpendingAnalysis
          category={data.category}
          loading={isLoading}
          error={sectionErrors.category}
        />
        <RecentActivity
          transactions={data.recentTransactions}
          loading={isLoading}
          error={sectionErrors.recentTransactions}
        />
      </section>
      <section className="dash-secondary-grid">
        <RecommendedProduct
          bookmarks={data.productBookmarks}
          loading={isLoading}
          error={sectionErrors.productBookmarks}
        />
        <PolicyRecommendations
          bookmarks={data.policyBookmarks}
          loading={isLoading}
          error={sectionErrors.policyBookmarks}
        />
        <SpendingTipCard
          coaching={data.coaching}
          loading={isLoading}
          error={sectionErrors.coaching}
        />
      </section>
    </main>
  );
}

export default Dashboard;
