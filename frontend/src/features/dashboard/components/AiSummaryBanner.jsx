import { aiSummary } from "../../../constants/mockDashboard";

function AiSummaryBanner() {
  return (
    <div className="ai-summary-banner">
      <h3>AI 추천 요약</h3>
      <p>
        현재 {aiSummary.productCount}개의 금융상품과 {aiSummary.policyCount}개의
        정책이 추천되었어요!
      </p>
      <div className="ai-summary-tags">
        <span>추천 상품 {aiSummary.productCount}</span>
        <span>추천 정책 {aiSummary.policyCount}</span>
        <span>맞춤 소비 팁 {aiSummary.tipCount}</span>
      </div>
    </div>
  );
}

export default AiSummaryBanner;
