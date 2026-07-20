import axios from "axios";

const axiosInstance = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL,
  withCredentials: true,
});

const PUBLIC_AUTH_PATHS = new Set([
  "/api/auth/signup",
  "/api/auth/login",
  "/api/auth/refresh",
  "/api/auth/logout",
  "/api/auth/email/code",
  "/api/auth/email/code/verify",
]);

const isPublicAuthPath = (url = "") => PUBLIC_AUTH_PATHS.has(url.split("?")[0]);

axiosInstance.interceptors.request.use((config) => {
  if (isPublicAuthPath(config.url) || config.skipAuth) {
    delete config.headers?.Authorization;
    return config;
  }

  const token = localStorage.getItem("accessToken");
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

/*
 * Access Token(30분) 만료로 401이 오면 Refresh rotation(/api/auth/refresh) 후
 * 원 요청을 1회 재시도한다. 동시에 여러 요청이 401을 받아도 refresh는 한 번만 나간다.
 * auth 경로 자신의 401(로그인 실패 등)은 재시도 대상이 아니다.
 */
let refreshPromise = null;

/**
 * Refresh Token까지 만료·폐기된 경우(진짜 세션 종료) AuthProvider에 알린다.
 * axios 인터셉터는 React 상태에 직접 접근할 수 없어 구독 방식으로 연결한다.
 */
let onSessionExpired = null;

export const setSessionExpiredHandler = (handler) => {
  onSessionExpired = handler;
};

axiosInstance.interceptors.response.use(
  (response) => response,
  async (error) => {
    const original = error.config;
    if (
      error.response?.status !== 401 ||
      !original ||
      original._retry ||
      original.skipAuth ||
      isPublicAuthPath(original.url)
    ) {
      return Promise.reject(error);
    }
    original._retry = true;
    try {
      refreshPromise ??= axiosInstance.post("/api/auth/refresh").finally(() => {
        refreshPromise = null;
      });
      const res = await refreshPromise;
      const accessToken = res.data?.data?.accessToken;
      if (!accessToken) {
        localStorage.removeItem("accessToken");
        onSessionExpired?.();
        return Promise.reject(error);
      }
      localStorage.setItem("accessToken", accessToken);
      return axiosInstance(original);
    } catch {
      localStorage.removeItem("accessToken");
      onSessionExpired?.();
      return Promise.reject(error);
    }
  },
);

export default axiosInstance;
