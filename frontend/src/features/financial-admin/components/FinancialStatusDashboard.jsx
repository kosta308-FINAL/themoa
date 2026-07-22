const number = (value) => Number(value ?? 0).toLocaleString();

/** 마지막 수집 시각. 오프셋 없는 서버 시각이라 로컬로 파싱·표시해 숫자를 그대로 보존한다. */
const formatDateTime = (isoString) => {
  if (!isoString) {
    return null;
  }
  const collectedAt = new Date(isoString);
  if (Number.isNaN(collectedAt.getTime())) {
    return null;
  }
  return collectedAt.toLocaleString("ko-KR", {
    month: "long",
    day: "numeric",
    hour: "2-digit",
    minute: "2-digit",
    hour12: false,
  });
};

/**
 * 금융상품 현황 대시보드.
 * indexSynced는 훅이 계산한 값으로, null이면 인덱스 문서 수를 몰라 비교를 생략한 상태다.
 */
function FinancialStatusDashboard({
  status,
  loading,
  error,
  sellingTotal,
  indexSynced,
}) {
  if (loading) {
    return (
      <section className="fa-card fa-note">현황을 불러오고 있어요.</section>
    );
  }
  if (error) {
    return <div className="fa-alert fa-alert-danger">{error}</div>;
  }
  if (!status) {
    return null;
  }

  const collectedAt = formatDateTime(status.lastCollectedAt);
  const indexedCount = status.indexedDocumentCount;
  const missingLinks = status.bankLinks?.missing || 0;

  return (
    <section className="fa-card fa-status">
      <div className="fa-card-head">
        <div>
          <h2>현황</h2>
        </div>
        {indexSynced != null && (
          <span
            className={`fa-badge ${indexSynced ? "fa-badge-ok" : "fa-badge-warn"}`}
          >
            {indexSynced ? "인덱스 최신 상태" : "인덱스 갱신 필요"}
          </span>
        )}
      </div>

      <div className="fa-stat-grid">
        <div className="fa-stat">
          <span>예·적금 (판매중)</span>
          <strong>{number(status.savings?.selling)}</strong>
          <small>전체 {number(status.savings?.total)}건</small>
        </div>
        <div className="fa-stat">
          <span>대출 (판매중)</span>
          <strong>{number(status.loans?.selling)}</strong>
          <small>전체 {number(status.loans?.total)}건</small>
        </div>
        <div className="fa-stat">
          <span>벡터 인덱스 문서</span>
          <strong>{indexedCount == null ? "-" : number(indexedCount)}</strong>
          <small>판매중 합계 {number(sellingTotal)}건</small>
        </div>
        <div className="fa-stat">
          <span>마지막 수집</span>
          <strong className={collectedAt ? "fa-stat-text" : "fa-stat-muted"}>
            {collectedAt || "수집 이력 없음"}
          </strong>
        </div>
        <div className="fa-stat">
          <span>검색 방식</span>
          <strong className="fa-stat-text">
            {status.vectorSearchEnabled ? "벡터 + 키워드" : "키워드 검색만"}
          </strong>
          {!status.vectorSearchEnabled && (
            <span className="fa-badge fa-badge-warn">
              키워드 검색만 동작 중
            </span>
          )}
        </div>
        <div className="fa-stat">
          <span>은행 공식 링크</span>
          <strong>{number(status.bankLinks?.registered)}</strong>
          <small>미등록 {number(missingLinks)}곳</small>
        </div>
      </div>

      {indexSynced === false && (
        <div className="fa-note fa-note-warn">
          판매중 상품 {number(sellingTotal)}건과 인덱스 {number(indexedCount)}
          건이 달라요. 수집 후 인덱스 갱신이 필요합니다.
        </div>
      )}

      {missingLinks > 0 && (
        <div className="fa-note">
          {number(missingLinks)}개 은행이 공식 링크 없이 검색 링크로 나가고
          있어요.{" "}
          <a className="fa-inline-link" href="#bank-links">
            은행 링크 관리로 이동 →
          </a>
        </div>
      )}
    </section>
  );
}

export default FinancialStatusDashboard;
