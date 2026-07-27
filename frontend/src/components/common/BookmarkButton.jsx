import "./BookmarkButton.css";

/**
 * 상품 카드의 북마크 토글 버튼. 비어 있으면 외곽선만, 저장돼 있으면 빨갛게 채워진다.
 */
function BookmarkButton({ bookmarked, busy, onToggle, size = 34 }) {
  const iconSize = Math.round(size * 0.58);
  return (
    <button
      type="button"
      className={`bookmark-button ${bookmarked ? "is-bookmarked" : ""}`}
      style={{ "--bookmark-size": `${size}px` }}
      onClick={onToggle}
      disabled={busy}
      aria-pressed={bookmarked}
      aria-label={bookmarked ? "북마크 해제" : "북마크 등록"}
      title={bookmarked ? "북마크 해제" : "북마크 등록"}
    >
      <svg
        viewBox="0 0 24 24"
        width={iconSize}
        height={iconSize}
        aria-hidden="true"
      >
        <path
          d="M6 3.5h12a1 1 0 0 1 1 1V21l-7-4-7 4V4.5a1 1 0 0 1 1-1Z"
          fill={bookmarked ? "currentColor" : "none"}
          stroke="currentColor"
          strokeWidth="1.8"
          strokeLinejoin="round"
        />
      </svg>
    </button>
  );
}

export default BookmarkButton;
