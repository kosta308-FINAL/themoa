import { useState } from "react";
import BankBadge from "../../../components/common/BankBadge";
import BookmarkButton from "../../../components/common/BookmarkButton";
import SavingsSubscriptionModal from "../../../components/common/SavingsSubscriptionModal";
import { SAVINGS_PRODUCT } from "../../../utils/bookmarkTarget";

const won = (value) => `${Number(value).toLocaleString("ko-KR")}원`;

const typeLabel = (type) => (type === "DEPOSIT" ? "정기예금" : "적금");

const homepageSearchUrl = (company) =>
  `https://search.naver.com/search.naver?query=${encodeURIComponent(
    `${company} 공식 홈페이지`,
  )}`;

/**
 * 추천 상품 카드 1건. 백엔드 Recommendation DTO를 그대로 렌더링한다.
 * 금리·기간·예상만기는 상단 칩으로 모아 밀도를 높이고, 추천 이유는 전부 보여준다.
 */
function RecommendResultCard({ item, rank, bookmarks }) {
  const [registerOpen, setRegisterOpen] = useState(false);
  const rotating = item.productName?.includes("회전");
  // 추천은 예·적금만 다루므로 북마크 대상 타입은 항상 SAVINGS_PRODUCT다.
  const canBookmark = item.id != null;
  const reasons = item.reasons || [];

  return (
    <article className="rec-card">
      <div className="rec-card-top">
        <BankBadge companyName={item.company} size={38} />
        <div className="rec-card-titles">
          <span className="rec-company">{item.company}</span>
          <h3 className="rec-name">
            {item.productName}
            {rotating && (
              <span className="rec-badge-warn">회전식·금리변동</span>
            )}
          </h3>
        </div>
        <div className="rec-card-top-right">
          <span className="rec-rank">{rank}위</span>
          {canBookmark && (
            <BookmarkButton
              bookmarked={bookmarks.isBookmarked(SAVINGS_PRODUCT, item.id)}
              busy={bookmarks.isBusy(SAVINGS_PRODUCT, item.id)}
              onToggle={() =>
                bookmarks.toggleBookmark(SAVINGS_PRODUCT, item.id)
              }
            />
          )}
        </div>
      </div>

      <div className="rec-chips">
        <span className="rec-chip">{typeLabel(item.type)}</span>
        {item.bestRate != null && (
          <span className="rec-chip rec-chip-rate">
            최고 {item.bestRate}%
            {item.bestRateTerm != null && ` · ${item.bestRateTerm}개월`}
          </span>
        )}
        {item.maturityAmountWon != null && (
          <span className="rec-chip">
            예상만기 {won(item.maturityAmountWon)}
          </span>
        )}
        <span className="rec-chip rec-chip-muted">점수 {item.score}</span>
        <span className="rec-chip rec-chip-muted">🛡 예금자보호</span>
      </div>

      {item.goalMonthlyWon != null && (
        <p className="rec-line rec-line-goal">
          🎯 목표달성 최소금액 매월 <strong>{won(item.goalMonthlyWon)}</strong>
          {item.goalMaturityAmountWon != null &&
            ` (만기 약 ${won(item.goalMaturityAmountWon)})`}
        </p>
      )}

      {item.llmReason && (
        <p className="rec-ai">
          <b>🤖 AI 총평</b> {item.llmReason}
        </p>
      )}

      {reasons.length > 0 && (
        <>
          <p className="rec-reasons-label">추천 이유</p>
          <ul className="rec-reasons">
            {reasons.map((reason, index) => (
              <li key={`${rank}-${index}`}>{reason}</li>
            ))}
          </ul>
        </>
      )}

      <div className="rec-actions">
        {canBookmark && (
          <button
            type="button"
            className="rec-register"
            onClick={() => setRegisterOpen(true)}
          >
            가입 등록
          </button>
        )}
        <a
          className="rec-link"
          href={homepageSearchUrl(item.company)}
          target="_blank"
          rel="noreferrer"
        >
          🔗 {item.company} 홈페이지 찾기
        </a>
      </div>

      {registerOpen && (
        <SavingsSubscriptionModal
          productId={item.id}
          onClose={() => setRegisterOpen(false)}
          onCreated={(message) => bookmarks.showToast?.(message)}
        />
      )}
    </article>
  );
}

export default RecommendResultCard;
