import { useEffect, useRef, useState } from "react";
import { createPortal } from "react-dom";
import BookmarkButton from "../../../components/common/BookmarkButton";
import DashboardIcon from "../../../components/common/DashboardIcon";
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

const dash = (value) => value || "-";

/**
 * 금융상품 검색 결과 상세. 정책 검색의 상세 모달과 같은 구조(백드롭·ESC·본문 스크롤 잠금)를
 * 쓴다. 검색 결과에 이미 전체 정보가 있어 별도 API 호출 없이 선택한 항목을 그대로 보여준다.
 */
function FinancialProductDetailModal({ item, bookmarks, onClose }) {
  const closeButtonRef = useRef(null);
  const previouslyFocusedRef = useRef(null);
  const [registerOpen, setRegisterOpen] = useState(false);

  const open = Boolean(item);
  const isLoan = item ? LOAN_TYPES.has(item.productType) : false;
  const typeLabel = item
    ? PRODUCT_TYPE_LABELS[item.productType] || item.productType
    : "";
  const rateLabel = isLoan ? "최저금리" : "최고금리";
  const targetType = item ? bookmarkTargetTypeOf(item.productType) : null;
  const canBookmark = item?.id != null;
  // 가입 등록은 예·적금만(대출은 만기·월납입 개념이 없어 제외).
  const canRegister = canBookmark && !isLoan;
  const bookmarked = canBookmark
    ? bookmarks.isBookmarked(targetType, item.id)
    : false;
  const bookmarkBusy = canBookmark
    ? bookmarks.isBusy(targetType, item.id)
    : false;

  useEffect(() => {
    if (!open) return undefined;
    previouslyFocusedRef.current = document.activeElement;
    const timer = setTimeout(() => closeButtonRef.current?.focus(), 0);
    const handleKeyDown = (event) => {
      if (event.key === "Escape") onClose();
    };
    document.addEventListener("keydown", handleKeyDown);
    const previousOverflow = document.body.style.overflow;
    document.body.style.overflow = "hidden";
    return () => {
      clearTimeout(timer);
      document.removeEventListener("keydown", handleKeyDown);
      document.body.style.overflow = previousOverflow;
      previouslyFocusedRef.current?.focus?.();
    };
  }, [onClose, open]);

  if (!open) {
    return null;
  }

  return createPortal(
    <div
      className="fs-detail-modal-backdrop"
      role="presentation"
      onMouseDown={onClose}
    >
      <section
        className="fs-detail-modal"
        role="dialog"
        aria-modal="true"
        aria-labelledby="fs-detail-title"
        onMouseDown={(event) => event.stopPropagation()}
      >
        <div className="fs-detail-modal-head">
          <strong>상품 상세</strong>
          <button
            type="button"
            className="fs-detail-close"
            ref={closeButtonRef}
            onClick={onClose}
            aria-label="닫기"
          >
            <DashboardIcon name="x" size={16} />
          </button>
        </div>

        <div className="fs-detail-badges">
          {item.discontinued && (
            <span className="fs-eyebrow fs-eyebrow-discontinued">
              판매종료
            </span>
          )}
          <span className="fs-eyebrow">{typeLabel}</span>
        </div>
        <h2 id="fs-detail-title">{item.productName}</h2>
        <p className="fs-detail-company">
          <DashboardIcon name="building" size={14} />
          {dash(item.companyName)}
        </p>

        {item.representativeRate != null && (
          <div className="fs-detail-rate">
            <span className="fs-detail-rate-label">{rateLabel}</span>
            <strong className="fs-detail-rate-value">
              {item.representativeRate}
              <em>%</em>
            </strong>
            {item.representativeTermMonth != null && (
              <span className="fs-detail-rate-term">
                {item.representativeTermMonth}개월
              </span>
            )}
          </div>
        )}

        <dl className="fs-detail-list">
          <div>
            <dt>
              <DashboardIcon name="card" size={13} />
              가입방법
            </dt>
            <dd>{dash(item.joinMethod)}</dd>
          </div>
        </dl>

        {/* 파싱된 우대조건이 있으면 체크리스트로, 없으면 원문 폴백. */}
        {item.conditions?.length > 0 ? (
          <div className="fs-detail-section">
            <p className="fs-detail-section-title">우대금리 조건</p>
            <ul className="fs-conditions">
              {item.conditions.map((condition, index) => (
                <li key={index}>
                  <DashboardIcon name="check" size={13} />
                  <span>{condition.description}</span>
                  {condition.rateBonus > 0 && (
                    <em>+{condition.rateBonus}%p</em>
                  )}
                </li>
              ))}
            </ul>
          </div>
        ) : (
          item.specialCondition && (
            <div className="fs-detail-section">
              <p className="fs-detail-section-title">우대금리 조건</p>
              <p className="fs-special">{item.specialCondition}</p>
            </div>
          )
        )}

        {item.matchReason && (
          <p className="fs-reason">
            <b>왜 나왔나요?</b> {item.matchReason}
          </p>
        )}

        <div className="fs-detail-actions">
          {canBookmark && (
            <BookmarkButton
              bookmarked={bookmarked}
              busy={bookmarkBusy}
              onToggle={() => bookmarks.toggleBookmark(targetType, item.id)}
            />
          )}
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
              className="fs-official-link"
              href={item.officialUrl}
              target="_blank"
              rel="noreferrer"
            >
              공식 사이트로 이동
              <DashboardIcon name="chevron-right" size={16} />
            </a>
          )}
        </div>
      </section>

      {registerOpen && (
        <SavingsSubscriptionModal
          productId={item.id}
          onClose={() => setRegisterOpen(false)}
          onCreated={(message) => bookmarks.showToast?.(message)}
        />
      )}
    </div>,
    document.body,
  );
}

export default FinancialProductDetailModal;
