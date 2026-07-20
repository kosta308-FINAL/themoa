/**
 * 서버 공통 오류 응답({ success: false, code, message })에서 사용자에게 보여줄 문구를 꺼낸다.
 * 서버 message가 이미 한국어 사용자 문구이므로 그대로 쓰고, 네트워크 오류 등만 fallback으로 대체한다.
 */
export function getApiErrorMessage(
  error,
  fallback = "요청을 처리하지 못했어요. 잠시 후 다시 시도해 주세요.",
) {
  return error?.response?.data?.message || fallback;
}
