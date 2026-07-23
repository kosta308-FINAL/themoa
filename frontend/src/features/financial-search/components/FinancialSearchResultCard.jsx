import { useState } from "react";
import BookmarkButton from "../../../components/common/BookmarkButton";
import SavingsSubscriptionModal from "../../../components/common/SavingsSubscriptionModal";
import { bookmarkTargetTypeOf } from "../../../utils/bookmarkTarget";

const PRODUCT_TYPE_LABELS = {
  DEPOSIT: "정기예금",
  SAVING: "적금",
  MORTGAGE: "주택담보대출",
  RENT: "전세자금대출",
  CREDIT: "개인신용대출",
};

const LOAN_TYPES = new Set(["MORTGAGE", "RENT", "CREDIT"]);

/**
 * 검색 결과 카드 1건. 대표금리 기준이 상품 성격에 따라 달라서(예·적금=최고금리, 대출=최저금리)
 * 라벨도 그에 맞춰 바꿔 표시한다.
 */
function FinancialSearchResultCard({ item, bookmarks }) {
  const [registerOpen, setRegisterOpen] = useState(false);
  const typeLabel = PRODUCT_TYPE_LABELS[item.productType] || item.productType;
  const isLoan = LOAN_TYPES.has(item.productType);
  const rateLabel = isLoan ? "최저" : "최고";
  const targetType = bookmarkTargetTypeOf(item.productType);
  const canBookmark = item.id != null;
  // 가입 등록은 예·적금만(대출은 만기·월납입 개념이 없어 제외).
  const canRegister = canBookmark && !isLoan;

  return (
    <article className="fs-card">
      <div className="fs-card-head">
        <span className={`fs-type ${isLoan ? "fs-type-loan" : ""}`}>
          {typeLabel}
        </span>
        <div className="fs-card-head-right">
          {item.representativeRate != null && (
            <span className="fs-rate">
              {rateLabel} <strong>{item.representativeRate}%</strong>
              {item.representativeTermMonth != null &&
                ` · ${item.representativeTermMonth}개월`}
            </span>
          )}
          {canBookmark && (
            <BookmarkButton
              bookmarked={bookmarks.isBookmarked(targetType, item.id)}
              busy={bookmarks.isBusy(targetType, item.id)}
              onToggle={() => bookmarks.toggleBookmark(targetType, item.id)}
            />
          )}
        </div>
      </div>

      <h3 className="fs-name">
        <span className="fs-company">{item.companyName}</span>
        {item.productName}
      </h3>

      {item.joinMethod && (
        <p className="fs-join">가입방법 · {item.joinMethod}</p>
      )}

      {item.specialCondition && (
        <p className="fs-special">{item.specialCondition}</p>
      )}

      {item.matchReason && (
        <p className="fs-reason">
          <b>왜 나왔나요?</b> {item.matchReason}
        </p>
      )}

      <div className="fs-actions">
        {canRegister && (
          <button
            type="button"
            className="fs-register"
            onClick={() => setRegisterOpen(true)}
          >
            가입 등록
          </button>
        )}
        {item.officialUrl && (
          <a
            className="fs-link"
            href={item.officialUrl}
            target="_blank"
            rel="noreferrer"
          >
            🔗 {item.companyName} 사이트로 이동
          </a>
        )}
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

export default FinancialSearchResultCard;
