import BankBadge from "../../../components/common/BankBadge";
import BookmarkButton from "../../../components/common/BookmarkButton";
import { bookmarkTargetTypeOf } from "../../../utils/bookmarkTarget";

const PRODUCT_TYPE_LABELS = {
  DEPOSIT: "정기예금",
  SAVING: "적금",
  MORTGAGE: "주택담보대출",
  RENT: "전세자금대출",
  CREDIT: "개인신용대출",
};

const LOAN_TYPES = new Set(["MORTGAGE", "RENT", "CREDIT"]);

const dash = (value) => value || "-";

/**
 * 검색 결과 카드 1건. 정책 검색 결과 카드와 같은 패턴 — 클릭 가능한 목록 행 하나로
 * 압축해 보여주고, 우대조건·가입등록 같은 자세한 내용은 상세 모달에서 다룬다.
 * 북마크는 우측 상단에 겹쳐서 바로 토글할 수 있게 한다(카드 클릭과 겹치지 않도록
 * 카드 전체를 <button>으로 두지 않고, 안쪽 콘텐츠만 별도 버튼으로 감싼다).
 */
function FinancialSearchResultCard({ item, active, bookmarks, onOpen }) {
  const typeLabel = PRODUCT_TYPE_LABELS[item.productType] || item.productType;
  const isLoan = LOAN_TYPES.has(item.productType);
  const rateLabel = isLoan ? "최저" : "최고";
  const targetType = bookmarkTargetTypeOf(item.productType);
  const canBookmark = item.id != null;

  return (
    <article className={`fs-result-card${active ? " active" : ""}`}>
      {canBookmark && (
        <div className="fs-result-bookmark">
          <BookmarkButton
            size={28}
            bookmarked={bookmarks.isBookmarked(targetType, item.id)}
            busy={bookmarks.isBusy(targetType, item.id)}
            onToggle={() => bookmarks.toggleBookmark(targetType, item.id)}
          />
        </div>
      )}

      <button
        type="button"
        className="fs-result-hit"
        onClick={() => onOpen(item)}
      >
        <div className="fs-result-title-row">
          <div className="fs-result-title-main">
            <BankBadge companyName={item.companyName} size={30} />
            <div>
              <strong>{item.productName}</strong>
              <span className="fs-result-company">{item.companyName}</span>
            </div>
          </div>
          {item.discontinued ? (
            <span className="fs-result-status fs-result-status-discontinued">
              판매종료
            </span>
          ) : (
            <span className={`fs-result-status${isLoan ? " loan" : ""}`}>
              {typeLabel}
            </span>
          )}
        </div>

        {item.matchReason && <p>{dash(item.matchReason)}</p>}

        <div className="fs-result-meta-row">
          {item.representativeRate != null && (
            <span className="fs-result-rate">
              {rateLabel} {item.representativeRate}%
              {item.representativeTermMonth != null &&
                ` · ${item.representativeTermMonth}개월`}
            </span>
          )}
          {item.joinMethod && <span>가입방법 {item.joinMethod}</span>}
        </div>
      </button>
    </article>
  );
}

export default FinancialSearchResultCard;
