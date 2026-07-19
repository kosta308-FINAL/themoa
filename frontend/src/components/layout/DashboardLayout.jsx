import { Outlet } from "react-router-dom";
import DashboardTopNav from "./DashboardTopNav";
import DashboardFooter from "./DashboardFooter";
import "../../features/dashboard/Dashboard.css";
import "./DashboardLayout.css";

function DashboardLayout() {
  return (
    <div className="dashboard dashboard-shell">
      <DashboardTopNav />
      <Outlet />
      <DashboardFooter />
    </div>
  );
}

export default DashboardLayout;
