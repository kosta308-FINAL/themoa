import BookmarkButton from "../../../../components/common/BookmarkButton";

const dash = (value) => value || "-";

const formatDate = (value) => {
  if (!value) return "";
  return value.replaceAll("-", ".");
};

const periodText = (item) => {
  if (item.alwaysOpen) return "상시 신청";
  if (item.applicationEndDate) return `${formatDate(item.applicationEndDate)} 마감`;
  if (item.applicationStartDate) return `${formatDate(item.applicationStartDate)}부터 신청`;
  return "신청 기간 확인 필요";
};

const ageText = (item) => {
  if (item.minAge == null && item.maxAge == null) return "";
  if (item.minAge != null && item.maxAge != null) return `만 ${item.minAge}~${item.maxAge}세`;
  if (item.minAge != null) return `만 ${item.minAge}세 이상`;
  return `만 ${item.maxAge}세 이하`;
};

function PolicyRecommendationTile({
  item,
  active,
  bookmarked,
  bookmarkBusy,
  onBookmarkToggle,
  onOpenDetail,
}) {
  const policyId = item.policyId;
  const metaItems = [
    ageText(item),
    item.applicationStatus,
    periodText(item),
  ].filter(Boolean).slice(0, 3);

  const handleOpenDetail = () => {
    onOpenDetail(policyId);
  };

  const handleKeyDown = (event) => {
    if (event.key !== "Enter" && event.key !== " ") return;
    event.preventDefault();
    handleOpenDetail();
  };

  const handleBookmarkToggle = (event) => {
    event.stopPropagation();
    onBookmarkToggle(policyId);
  };

  const handleDetailButtonClick = (event) => {
    event.stopPropagation();
    handleOpenDetail();
  };

  return (
    <article
      className={`policy-recommendation-tile${active ? " active" : ""}`}
      role="button"
      tabIndex={0}
      aria-label={`${dash(item.title)} 상세보기`}
      onClick={handleOpenDetail}
      onKeyDown={handleKeyDown}
    >
      <div className="policy-recommendation-tile-head">
        <span className="policy-recommendation-region">{dash(item.region)}</span>
        <div className="policy-recommendation-bookmark" onClick={(event) => event.stopPropagation()}>
          <BookmarkButton
            bookmarked={bookmarked}
            busy={bookmarkBusy}
            onToggle={handleBookmarkToggle}
          />
        </div>
      </div>

      <div className="policy-recommendation-tile-body">
        <h3 className="policy-recommendation-title">{dash(item.title)}</h3>
        <p className="policy-recommendation-tile-summary">{dash(item.summary)}</p>
        <div className="policy-recommendation-meta">
          {metaItems.map((meta) => (
            <span key={meta}>{meta}</span>
          ))}
        </div>
        {item.matchReason && (
          <div className="policy-recommendation-reason-box">
            <span>추천 이유</span>
            <p>{item.matchReason}</p>
          </div>
        )}
      </div>

      <div className="policy-recommendation-tile-footer">
        <button type="button" onClick={handleDetailButtonClick}>
          상세보기
        </button>
      </div>
    </article>
  );
}

export default PolicyRecommendationTile;
