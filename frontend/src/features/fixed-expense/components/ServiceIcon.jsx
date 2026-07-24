/** 서비스 이니셜 아바타. 톤별 옅은 배경 + 진한 텍스트(패널 타이틀 아이콘과 동일한 배지 톤). */
function ServiceIcon({ tone, children }) {
  return (
    <span className={`fx-service-icon fx-tone-${tone || "green"}`}>
      {children}
    </span>
  );
}

export default ServiceIcon;
