import { usePolicyBookmarks } from "../../policy/hooks/usePolicyBookmarks";
import "./PolicyBookmarksCard.css";

const dash = (value) => value || "미정";

const periodText = (item) => {
  if (item.alwaysOpen) return "상시 신청";
  if (!item.startDate && !item.dueDate) return "미정";
  return `${dash(item.startDate)} ~ ${dash(item.dueDate)}`;
};

function PolicyBookmarksCard() {
  const bookmarks = usePolicyBookmarks();

  return (
    <section className="mp-card mp-policy-bookmark-card">
      <div className="mp-card-head">
        <div>
          <h2>관심 정책</h2>
          <p className="mp-card-sub">총 {bookmarks.items.length}개</p>
        </div>
      </div>

      {bookmarks.loading && (
        <p className="mp-empty">관심 정책을 불러오고 있어요.</p>
      )}

      {!bookmarks.loading && bookmarks.error && (
        <div className="mp-policy-bookmark-error">
          <span>{bookmarks.error}</span>
          <button type="button" onClick={bookmarks.loadBookmarks}>
            다시 시도
          </button>
        </div>
      )}

      {!bookmarks.loading && !bookmarks.error && bookmarks.items.length === 0 && (
        <div className="mp-policy-bookmark-empty">
          <p>아직 저장한 정책이 없어요.</p>
          <span>정책 검색에서 관심 있는 정책을 저장해 보세요.</span>
        </div>
      )}

      {!bookmarks.loading && !bookmarks.error && bookmarks.items.length > 0 && (
        <div className="mp-policy-bookmark-list">
          {bookmarks.items.map((item) => {
            const removing = bookmarks.busyPolicyId === item.policyId;
            return (
              <article className="mp-policy-bookmark-item" key={item.bookmarkId}>
                <div className="mp-policy-bookmark-head">
                  <div>
                    <h3>{item.title}</h3>
                    <div className="mp-policy-bookmark-meta">
                      <span>{dash(item.agencyName)}</span>
                      <span>{dash(item.category)}</span>
                      <span>{dash(item.policyStatus)}</span>
                      {!item.active && (
                        <span className="mp-policy-bookmark-badge">비활성</span>
                      )}
                    </div>
                  </div>
                </div>
                <p className="mp-policy-bookmark-period">{periodText(item)}</p>
                <p className="mp-policy-bookmark-summary">{dash(item.summary)}</p>
                <div className="mp-policy-bookmark-actions">
                  {item.officialUrl && (
                    <a href={item.officialUrl} target="_blank" rel="noreferrer">
                      공식 링크
                    </a>
                  )}
                  <button
                    type="button"
                    disabled={removing}
                    onClick={() => bookmarks.removeBookmark(item.policyId)}
                  >
                    {removing ? "처리 중..." : "즐겨찾기 해제"}
                  </button>
                </div>
              </article>
            );
          })}
        </div>
      )}
    </section>
  );
}

export default PolicyBookmarksCard;
