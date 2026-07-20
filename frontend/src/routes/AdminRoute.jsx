import { Navigate, Outlet } from "react-router-dom";
import { useAuth } from "../hooks/useAuth";

/** 관리자 화면 가드. 실제 인가는 서버(SecurityConfig /api/admin/**)가 최종 판단하며, 이 가드는 화면 노출만 막는다. */
function AdminRoute() {
  const { isAuthenticated, isAdmin } = useAuth();
  if (!isAuthenticated) {
    return <Navigate to="/login" replace />;
  }
  return isAdmin ? <Outlet /> : <Navigate to="/dashboard" replace />;
}

export default AdminRoute;
