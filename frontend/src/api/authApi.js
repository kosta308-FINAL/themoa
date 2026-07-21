import axiosInstance from "./axiosInstance";

/** 회원가입 전 이메일 인증 코드 발송. 재발송은 60초 쿨다운(429). */
export const sendEmailCode = (email) =>
  axiosInstance.post("/api/auth/email/code", { email });

/** 6자리 코드 검증. 통과해야 같은 이메일로 가입할 수 있다. */
export const verifyEmailCode = (email, code) =>
  axiosInstance.post("/api/auth/email/code/verify", { email, code });

/**
 * 일반 회원가입. 성공 시 201 + { accessToken, expiresIn } (Refresh는 HttpOnly 쿠키).
 * @param {{ email, password, passwordConfirm, nickname, gender: 'MALE'|'FEMALE', birthDate: 'YYYY-MM-DD',
 *   agreedServiceTerms: boolean, agreedPrivacyPolicy: boolean, agreedDataCollection: boolean }} payload
 */
export const signup = (payload) =>
  axiosInstance.post("/api/auth/signup", payload);

/** 이메일·비밀번호 로그인. 성공 시 { accessToken, expiresIn }. */
export const login = (email, password) =>
  axiosInstance.post("/api/auth/login", { email, password });

/** 현재 기기 로그아웃. 이미 폐기된 토큰이어도 서버가 멱등 처리한다(204). */
export const logout = () => axiosInstance.post("/api/auth/logout");

/** Refresh rotation. 쿠키의 Refresh Token으로 새 Access Token을 받는다. */
export const refresh = () => axiosInstance.post("/api/auth/refresh");
