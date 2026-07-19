import { Navigate, Outlet } from "react-router-dom";
import { useAuth } from "../hooks/useAuth";

/** 미인증 상태(최초 미로그인 또는 Refresh Token 만료로 인한 강제 로그아웃)면 로그인 화면으로 보낸다. */
function ProtectedRoute() {
  const { isAuthenticated } = useAuth();
  return isAuthenticated ? <Outlet /> : <Navigate to="/login" replace />;
}

export default ProtectedRoute;
