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

/**
 * 비밀번호 변경. 성공 시 이 기기를 포함한 전 세션이 즉시 무효화되어 재로그인이 필요하다.
 * @param {{ currentPassword, newPassword, newPasswordConfirm }} payload
 */
export const changePassword = (payload) =>
  axiosInstance.patch("/api/auth/password", payload);

/** 전체 기기 로그아웃. 이 회원의 모든 기기 세션이 즉시 무효화된다(이 기기 포함). */
export const logoutAllDevices = () =>
  axiosInstance.post("/api/auth/logout-all");

/**
 * 회원 탈퇴. 비밀번호 확인 후 즉시 처리되며 전 세션이 무효화되어 다시 로그인할 수 없다.
 * @param {{ password: string }} payload
 */
export const withdrawAccount = (payload) =>
  axiosInstance.delete("/api/auth/account", { data: payload });

/**
 * 아이디(이메일) 찾기. 닉네임+생년월일이 정확히 1건 일치할 때만 마스킹된 이메일을 돌려준다.
 * @returns {Promise<{ data: { data: { maskedEmail: string } } }>}
 */
export const findEmail = (nickname, birthDate) =>
  axiosInstance.post("/api/auth/find-email", { nickname, birthDate });

/** 비밀번호 찾기 1단계: 가입된 이메일로 인증 코드 발송. */
export const sendPasswordResetCode = (email) =>
  axiosInstance.post("/api/auth/password/reset/code", { email });

/** 비밀번호 찾기 2단계: 인증 코드 검증. */
export const verifyPasswordResetCode = (email, code) =>
  axiosInstance.post("/api/auth/password/reset/code/verify", { email, code });

/**
 * 비밀번호 찾기 3단계: 새 비밀번호로 재설정. 성공 시 전 세션이 무효화되어 재로그인이 필요하다.
 * @param {{ email, newPassword, newPasswordConfirm }} payload
 */
export const resetPassword = (payload) =>
  axiosInstance.post("/api/auth/password/reset", payload);

/**
 * 카카오 로그인 시작 URL. 버튼 클릭 시 axios가 아니라 전체 페이지 이동(window.location.href)으로
 * 써야 한다 — 카카오 동의 화면으로 브라우저 자체가 넘어가야 하기 때문이다.
 * ALB가 "/api/*"만 백엔드로 보내므로(distribution/distributionSetting.md §10.2) /api 하위 경로다.
 */
export const kakaoLoginUrl = () =>
  `${import.meta.env.VITE_API_BASE_URL ?? ""}/api/oauth2/authorization/kakao`;

/**
 * 카카오 콜백(/oauth/callback) 직후 1회 호출. 교환코드를 소비해 기존 회원이면 로그인을,
 * 신규 회원이면 추가정보 입력에 필요한 signupTicket·nickname을 받는다.
 * @returns {Promise<{ data: { data: { requiresSignup: boolean, token?: { accessToken, expiresIn },
 *   signupTicket?: string, nickname?: string } } }>}
 */
export const exchangeOAuthCode = (code) =>
  axiosInstance.post("/api/auth/oauth/exchange", { code }, { skipAuth: true });

/**
 * 카카오 신규 회원 가입 완료. 성공 시 { accessToken, expiresIn }(Refresh는 HttpOnly 쿠키)으로 자동 로그인.
 * @param {{ signupTicket, email, gender: 'MALE'|'FEMALE', birthDate: 'YYYY-MM-DD',
 *   agreedServiceTerms: boolean, agreedPrivacyPolicy: boolean, agreedDataCollection: boolean }} payload
 */
export const completeKakaoSignup = (payload) =>
  axiosInstance.post("/api/auth/oauth/kakao/complete-signup", payload, {
    skipAuth: true,
  });
