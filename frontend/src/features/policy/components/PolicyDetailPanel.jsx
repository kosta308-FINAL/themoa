import { useEffect, useRef } from "react";
import DashboardIcon from "../../../components/common/DashboardIcon";
import { usePolicyBookmarks } from "../hooks/usePolicyBookmarks";

const dash = (value) => value || "-";
const listText = (value) =>
  Array.isArray(value) && value.length ? value.join(", ") : "-";

function PolicyDetailPanel({ selected, detailLoading, onClose }) {
  const bookmarks = usePolicyBookmarks();
  const closeButtonRef = useRef(null);
  const previouslyFocusedRef = useRef(null);
  const selectedPolicyId = selected?.policyId;
  const bookmarked = selectedPolicyId
    ? bookmarks.isBookmarked(selectedPolicyId)
    : false;
  const bookmarkBusy =
    selectedPolicyId != null && bookmarks.busyPolicyId === selectedPolicyId;
  const bookmarkDisabled = bookmarks.loading || bookmarkBusy;
  const bookmarkLabel = bookmarkBusy
    ? "처리 중..."
    : bookmarks.loading
      ? "즐겨찾기 확인 중..."
      : bookmarked
        ? "즐겨찾기 해제"
        : "즐겨찾기 추가";

  const handleToggleBookmark = () => {
    if (selectedPolicyId == null) return;
    bookmarks.toggleBookmark(selectedPolicyId);
  };

  const open = detailLoading || Boolean(selected);

  useEffect(() => {
    if (!open) return undefined;
    previouslyFocusedRef.current = document.activeElement;
    const timer = setTimeout(() => {
      closeButtonRef.current?.focus();
    }, 0);
    const handleKeyDown = (event) => {
      if (event.key === "Escape") {
        onClose();
      }
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

  return (
    <div
      className="policy-detail-modal-backdrop"
      role="presentation"
      onMouseDown={onClose}
    >
      <section
        className="policy-detail-modal"
        role="dialog"
        aria-modal="true"
        aria-labelledby={selected ? "policy-detail-title" : undefined}
        aria-label={!selected ? "정책 상세 정보" : undefined}
        onMouseDown={(event) => event.stopPropagation()}
      >
        <div className="policy-detail-modal-head">
          <strong>정책 상세</strong>
          <button
            type="button"
            className="policy-detail-close"
            ref={closeButtonRef}
            onClick={onClose}
            aria-label="닫기"
          >
            <DashboardIcon name="x" size={16} />
          </button>
        </div>
        {detailLoading && (
          <div className="policy-empty">상세 정보를 불러오는 중입니다.</div>
        )}
        {!detailLoading && selected && (
          <>
            <p className="policy-eyebrow">{dash(selected.sourcePolicyId)}</p>
            <h2 id="policy-detail-title">{selected.title}</h2>
            <dl className="policy-detail-list">
              <div>
                <dt>기관</dt>
                <dd>{dash(selected.agencyName)}</dd>
              </div>
              <div>
                <dt>분야</dt>
                <dd>{dash(selected.category)}</dd>
              </div>
              <div>
                <dt>상태</dt>
                <dd>{dash(selected.status)}</dd>
              </div>
              <div>
                <dt>지역</dt>
                <dd>{listText(selected.regions)}</dd>
              </div>
              <div>
                <dt>요약</dt>
                <dd>{dash(selected.summary)}</dd>
              </div>
            </dl>
            <div className="policy-detail-actions">
              <button
                className={`policy-bookmark-button${bookmarked ? " active" : ""}`}
                type="button"
                disabled={bookmarkDisabled}
                onClick={handleToggleBookmark}
              >
                {bookmarkLabel}
              </button>
              {selected.officialUrl && (
                <a
                  className="policy-official-link"
                  href={selected.officialUrl}
                  target="_blank"
                  rel="noreferrer"
                >
                  공식 링크
                  <DashboardIcon name="chevron-right" size={16} />
                </a>
              )}
            </div>
            {bookmarks.error && (
              <p className="policy-bookmark-error">{bookmarks.error}</p>
            )}
          </>
        )}
      </section>
    </div>
  );
}

export default PolicyDetailPanel;
