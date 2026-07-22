function AiSummaryBanner({
  productBookmarkCount,
  policyBookmarkCount,
  coachingCount,
}) {
  return (
    <div className="ai-summary-banner">
      <h3>내 금융 요약</h3>
      <p>관심 상품과 정책, 현재 확인할 수 있는 소비 코칭을 모아봤어요.</p>
      <div className="ai-summary-tags">
        <span>관심 상품 {productBookmarkCount}</span>
        <span>관심 정책 {policyBookmarkCount}</span>
        <span>소비 코칭 {coachingCount}</span>
      </div>
    </div>
  );
}

export default AiSummaryBanner;
