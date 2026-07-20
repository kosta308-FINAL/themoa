import { useCallback, useEffect, useState } from "react";
import AuthContext from "./AuthContext";
import { logout as requestLogout } from "../api/authApi";
import { setSessionExpiredHandler } from "../api/axiosInstance";
import { isAdminAccessToken } from "../utils/accessToken";

export function AuthProvider({ children }) {
  const [isAuthenticated, setIsAuthenticated] = useState(() =>
    Boolean(localStorage.getItem("accessToken")),
  );
  const [isAdmin, setIsAdmin] = useState(() => isAdminAccessToken());

  /** Refresh Token까지 만료되어 axiosInstance가 세션을 강제 종료한 경우를 반영한다. */
  useEffect(() => {
    setSessionExpiredHandler(() => {
      setIsAuthenticated(false);
      setIsAdmin(false);
    });
    return () => setSessionExpiredHandler(null);
  }, []);

  /** 로그인·회원가입 성공 응답({ accessToken })으로 세션을 시작한다. */
  const login = useCallback(({ accessToken }) => {
    localStorage.setItem("accessToken", accessToken);
    setIsAuthenticated(true);
    setIsAdmin(isAdminAccessToken());
  }, []);

  /** 서버의 Refresh Token을 무효화하고 로컬 토큰을 지운다. 서버 호출이 실패해도 로컬 세션은 끝낸다. */
  const logout = useCallback(async () => {
    try {
      await requestLogout();
    } catch {
      // 이미 만료·폐기된 토큰이어도 로그아웃은 성공으로 취급한다
    } finally {
      localStorage.removeItem("accessToken");
      setIsAuthenticated(false);
      setIsAdmin(false);
    }
  }, []);

  return (
    <AuthContext.Provider value={{ isAuthenticated, isAdmin, login, logout }}>
      {children}
    </AuthContext.Provider>
  );
}

export default AuthProvider;
