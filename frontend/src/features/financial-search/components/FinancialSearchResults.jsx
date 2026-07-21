import FinancialSearchResultCard from "./FinancialSearchResultCard";

/**
 * 검색 결과 영역. 로딩·오류·검색 전·결과 없음(대안 검색어)·정상 목록을 분기한다.
 * message는 "검색어를 확장해서 찾았다" 같은 안내라 결과 유무와 상관없이 있으면 보여준다.
 */
function FinancialSearchResults({
  loading,
  error,
  data,
  searched,
  onSearch,
  bookmarks,
}) {
  if (loading) {
    return <div className="fs-state">금융상품을 찾고 있어요…</div>;
  }
  if (error) {
    return <div className="fs-alert fs-alert-danger">{error}</div>;
  }
  if (!searched) {
    return (
      <div className="fs-state">
        찾고 싶은 상품을 자연어로 입력해 보세요. 예금·적금·대출을 한 번에
        찾아드려요.
      </div>
    );
  }

  const results = data?.results || [];
  const suggestedQueries = data?.suggestedQueries || [];

  return (
    <div className="fs-results-area">
      {data?.message && (
        <div className="fs-alert fs-alert-info">{data.message}</div>
      )}

      {results.length === 0 ? (
        <div className="fs-state">
          <p>검색 결과가 없어요.</p>
          {suggestedQueries.length > 0 && (
            <div className="fs-suggested">
              <span className="fs-suggested-label">
                이렇게 찾아보는 건 어때요?
              </span>
              <div className="fs-suggested-chips">
                {suggestedQueries.map((suggested) => (
                  <button
                    key={suggested}
                    type="button"
                    className="fs-example"
                    onClick={() => onSearch(suggested)}
                  >
                    {suggested}
                  </button>
                ))}
              </div>
            </div>
          )}
        </div>
      ) : (
        <>
          <p className="fs-count">
            총 <strong>{data?.resultCount ?? results.length}</strong>건
          </p>
          <div className="fs-results">
            {results.map((item) => (
              <FinancialSearchResultCard
                key={`${item.productType}-${item.id}`}
                item={item}
                bookmarks={bookmarks}
              />
            ))}
          </div>
        </>
      )}
    </div>
  );
}

export default FinancialSearchResults;
