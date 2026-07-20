import { Navigate, Outlet } from "react-router-dom";
import { useAuth } from "../hooks/useAuth";

/** 관리자는 일반 대시보드 화면으로 들어오지 못하고 관리자 화면으로 보낸다. */
function UserOnlyRoute() {
  const { isAdmin } = useAuth();
  return isAdmin ? (
    <Navigate to="/admin/customer-service" replace />
  ) : (
    <Outlet />
  );
}

export default UserOnlyRoute;
