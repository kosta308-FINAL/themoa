import DashboardIcon from "../../../components/common/DashboardIcon";
import { popularSearches } from "../../../constants/mockDashboard";

function AiChatCard() {
  return (
    <div className="ai-search-card">
      <h3>AI 금융비서에게 물어보세요</h3>
      <p>금융, 정책, 소비에 대해 무엇이든 검색해보세요</p>
      <div className="ai-search-box">
        <DashboardIcon name="search" size={16} />
        <input type="text" placeholder="예) 청년 전세자금 대출 알려줘" />
      </div>
      <div className="ai-popular-searches">
        <span className="ai-popular-searches-label">인기 검색어</span>
        <div className="ai-popular-searches-tags">
          {popularSearches.map((term) => (
            <span key={term}>{term}</span>
          ))}
        </div>
      </div>
      <div className="ai-search-mascot">
        <span className="ai-search-mascot-face">
          <span className="ai-search-mascot-eye" />
          <span className="ai-search-mascot-eye" />
        </span>
        <span className="ai-search-mascot-bubble">?</span>
      </div>
    </div>
  );
}

export default AiChatCard;
