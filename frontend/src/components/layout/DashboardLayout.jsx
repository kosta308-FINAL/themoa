import { useEffect, useState } from "react";
import { Outlet } from "react-router-dom";
import DashboardTopNav from "./DashboardTopNav";
import DashboardFooter from "./DashboardFooter";
import RecoverySyncModal from "./RecoverySyncModal";
import { getSyncRecoveryStatus } from "../../api/spendingGuideApi";
import "../../features/dashboard/Dashboard.css";
import "./DashboardLayout.css";

function DashboardLayout() {
  const [showRecovery, setShowRecovery] = useState(false);

  useEffect(() => {
    let active = true;
    getSyncRecoveryStatus()
      .then((response) => {
        if (active && response?.returningAfterLongAbsence) {
          setShowRecovery(true);
        }
      })
      .catch(() => {
        // 복귀 여부 확인 실패는 조용히 넘어간다(다음 진입 시 다시 판정된다).
      });
    return () => {
      active = false;
    };
  }, []);

  return (
    <div className="dashboard dashboard-shell">
      <DashboardTopNav />
      <Outlet />
      <DashboardFooter />
      {showRecovery && (
        <RecoverySyncModal onClose={() => setShowRecovery(false)} />
      )}
    </div>
  );
}

export default DashboardLayout;
