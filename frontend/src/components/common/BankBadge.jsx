import "./BankBadge.css";

// 회사명에서 로고 대신 쓸 핵심 글자를 뽑는다. "주식회사"·"(주)" 접미/접두를 떼고 첫 글자.
// 예: "주식회사 카카오뱅크" → "카", "농협은행주식회사" → "농"
const initialOf = (companyName) => {
  const core = (companyName || "")
    .replace(/주식회사/g, "")
    .replace(/\(주\)/g, "")
    .trim();
  return core ? core[0] : "?";
};

// 회사명 해시로 색상(hue)을 자동 배정 — 같은 회사는 항상 같은 색.
const hueOf = (companyName) => {
  const text = companyName || "";
  let hash = 0;
  for (let i = 0; i < text.length; i += 1) {
    hash = (hash * 31 + text.charCodeAt(i)) % 360;
  }
  return hash;
};

/**
 * 은행/회사 이니셜 뱃지. 실제 로고 대신 회사명 첫 글자를 원형 뱃지로 보여준다.
 * 색은 회사명 해시로 자동 배정되어 은행별로 구분된다. 추천·검색·북마크·가입 카드에서 공통 사용.
 */
function BankBadge({ companyName, size = 40 }) {
  const hue = hueOf(companyName);
  return (
    <span
      className="bank-badge"
      style={{
        width: size,
        height: size,
        fontSize: Math.round(size * 0.42),
        background: `hsl(${hue}, 52%, 45%)`,
      }}
      aria-hidden="true"
    >
      {initialOf(companyName)}
    </span>
  );
}

export default BankBadge;
