import { useState } from "react";
import { getPolicyDetail } from "../../../../api/policyApi";
import BookmarkButton from "../../../../components/common/BookmarkButton";
import DashboardIcon from "../../../../components/common/DashboardIcon";
import { getApiErrorMessage } from "../../../../utils/apiError";

const dash = (value) => value || "-";
const listText = (value) =>
  Array.isArray(value) && value.length ? value.join(", ") : "-";

const formatDate = (value) => {
  if (!value) return "";
  return value.replaceAll("-", ".");
};

const periodText = (item) => {
  if (item.alwaysOpen) return "상시 신청";
  if (item.applicationEndDate)
    return `${formatDate(item.applicationEndDate)} 마감`;
  if (item.applicationStartDate)
    return `${formatDate(item.applicationStartDate)}부터 신청`;
  return "신청 기간 확인 필요";
};

const ageText = (item) => {
  if (item.minAge == null && item.maxAge == null) return "";
  if (item.minAge != null && item.maxAge != null)
    return `만 ${item.minAge}~${item.maxAge}세`;
  if (item.minAge != null) return `만 ${item.minAge}세 이상`;
  return `만 ${item.maxAge}세 이하`;
};

function PolicyRecommendationTile({
  item,
  rank,
  bookmarked,
  bookmarkBusy,
  onBookmarkToggle,
}) {
  const policyId = item.policyId;
  const [expanded, setExpanded] = useState(false);
  const [detail, setDetail] = useState(null);
  const [detailLoading, setDetailLoading] = useState(false);
  const [detailError, setDetailError] = useState("");

  const metaItems = [ageText(item), item.applicationStatus, periodText(item)]
    .filter(Boolean)
    .slice(0, 3);

  const handleToggleExpand = async () => {
    const next = !expanded;
    setExpanded(next);
    if (!next || detail || detailLoading) return;
    setDetailLoading(true);
    setDetailError("");
    try {
      const data = await getPolicyDetail(policyId);
      setDetail(data);
    } catch (error) {
      setDetailError(
        getApiErrorMessage(error, "정책 상세 정보를 불러오지 못했어요."),
      );
    } finally {
      setDetailLoading(false);
    }
  };

  const handleKeyDown = (event) => {
    if (event.key !== "Enter" && event.key !== " ") return;
    event.preventDefault();
    handleToggleExpand();
  };

  const handleBookmarkToggle = (event) => {
    event.stopPropagation();
    onBookmarkToggle(policyId);
  };

  return (
    <article
      className={`policy-recommendation-tile${expanded ? " active" : ""}`}
    >
      <div
        className="policy-recommendation-tile-row"
        role="button"
        tabIndex={0}
        aria-expanded={expanded}
        aria-label={`${dash(item.title)} 상세 ${expanded ? "접기" : "펼치기"}`}
        onClick={handleToggleExpand}
        onKeyDown={handleKeyDown}
      >
        <span
          className={`policy-recommendation-rank${rank <= 3 ? " top" : ""}`}
        >
          {rank}
        </span>

        <div className="policy-recommendation-tile-main">
          <div className="policy-recommendation-tile-head">
            <span className="policy-recommendation-region">
              {dash(item.region)}
            </span>
            <div className="policy-recommendation-meta">
              {metaItems.map((meta) => (
                <span key={meta}>{meta}</span>
              ))}
            </div>
          </div>

          <h3 className="policy-recommendation-title">{dash(item.title)}</h3>
          <p className="policy-recommendation-tile-summary">
            {dash(item.summary)}
          </p>

          {item.matchReason && (
            <p className="policy-recommendation-reason-inline">
              <span>추천 이유</span>
              {item.matchReason}
            </p>
          )}
        </div>

        <div
          className="policy-recommendation-tile-side"
          onClick={(event) => event.stopPropagation()}
        >
          <BookmarkButton
            bookmarked={bookmarked}
            busy={bookmarkBusy}
            onToggle={handleBookmarkToggle}
            size={40}
          />
          <button
            type="button"
            className={`policy-recommendation-toggle${expanded ? " open" : ""}`}
            aria-expanded={expanded}
            onClick={(event) => {
              event.stopPropagation();
              handleToggleExpand();
            }}
          >
            상세보기
            <DashboardIcon
              name="chevron-down"
              size={14}
              className="policy-recommendation-toggle-icon"
            />
          </button>
        </div>
      </div>

      <div className={`policy-recommendation-detail${expanded ? " open" : ""}`}>
        <div className="policy-recommendation-detail-inner">
          <div className="policy-recommendation-detail-content">
            {detailLoading && (
              <p className="policy-recommendation-detail-loading">
                상세 정보를 불러오는 중입니다.
              </p>
            )}
            {detailError && (
              <p className="policy-recommendation-error">{detailError}</p>
            )}
            {!detailLoading && !detailError && detail && (
              <>
                <dl className="policy-recommendation-detail-list">
                  <div>
                    <dt>기관</dt>
                    <dd>{dash(detail.agencyName)}</dd>
                  </div>
                  <div>
                    <dt>분야</dt>
                    <dd>{dash(detail.category)}</dd>
                  </div>
                  <div>
                    <dt>상태</dt>
                    <dd>{dash(detail.status)}</dd>
                  </div>
                  <div>
                    <dt>지역</dt>
                    <dd>{listText(detail.regions)}</dd>
                  </div>
                  <div>
                    <dt>요약</dt>
                    <dd>{dash(detail.summary)}</dd>
                  </div>
                </dl>
                {detail.officialUrl && (
                  <a
                    className="policy-official-link"
                    href={detail.officialUrl}
                    target="_blank"
                    rel="noreferrer"
                    onClick={(event) => event.stopPropagation()}
                  >
                    공식 링크
                    <DashboardIcon name="chevron-right" size={16} />
                  </a>
                )}
              </>
            )}
          </div>
        </div>
      </div>
    </article>
  );
}

export default PolicyRecommendationTile;
