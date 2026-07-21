/**
 * Access Token(JWT)의 role 클레임을 읽는다. 서버가 최종 인가를 하므로(SecurityConfig /api/admin/**),
 * 이 값은 관리자 전용 화면·메뉴 노출 여부를 결정하는 프론트 전용 판단에만 쓴다.
 */
export function getAccessTokenRole() {
  const token = localStorage.getItem("accessToken");
  if (!token) return null;
  const parts = token.split(".");
  if (parts.length !== 3) return null;
  try {
    const payload = JSON.parse(
      atob(parts[1].replace(/-/g, "+").replace(/_/g, "/")),
    );
    return payload.role || null;
  } catch {
    return null;
  }
}

export function isAdminAccessToken() {
  return getAccessTokenRole() === "ADMIN";
}
