import { mockUser } from "../../../constants/mockDashboard";

function DashboardTopbar() {
  return (
    <div className="dash-topbar">
      <div>
        <h1>안녕하세요, {mockUser.name}님! 👋</h1>
        <p>오늘도 똑똑한 금융 습관으로 한 걸음 더 나아가요.</p>
      </div>
      <span className="dash-updated">
        최근 업데이트: {mockUser.lastUpdated}
      </span>
    </div>
  );
}

export default DashboardTopbar;
