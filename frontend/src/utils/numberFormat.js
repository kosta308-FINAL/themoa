/**
 * 숫자 입력칸의 천 단위 콤마 표시용 헬퍼.
 * 화면에는 "4,000,000"으로 보여주고, 상태·전송에는 콤마 없는 숫자 문자열을 쓴다.
 */

/** 숫자/문자열을 천 단위 콤마 문자열로. 빈 값이면 빈 문자열. */
export const withCommas = (value) => {
  if (value === "" || value == null) {
    return "";
  }
  const digits = String(value).replace(/[^\d]/g, "");
  if (digits === "") {
    return "";
  }
  return Number(digits).toLocaleString("ko-KR");
};

/** 콤마 등 숫자가 아닌 문자를 제거해 순수 숫자 문자열로. */
export const stripCommas = (value) => String(value ?? "").replace(/[^\d]/g, "");
