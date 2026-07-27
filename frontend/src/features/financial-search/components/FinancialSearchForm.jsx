import DashboardIcon from "../../../components/common/DashboardIcon";

const SORT_OPTIONS = [
  { value: "RELEVANCE", label: "관련도순" },
  { value: "RATE_DESC", label: "금리 높은순" },
  { value: "RATE_ASC", label: "금리 낮은순" },
  { value: "TERM_ASC", label: "가입기간 짧은순" },
];

const EXAMPLES = [
  "20대 사회초년생이 넣기 좋은 적금",
  "금리 높은 정기예금",
  "비대면으로 가입할 수 있는 적금",
  "전세자금대출",
];

/**
 * 검색어·정렬 입력. 정책 검색의 "AI 챗봇" 히어로와 같은 구조(배지·타이틀·알약 검색바·예시)를
 * 쓰되 색은 블루 계열로 구분한다. 정렬을 바꾸면 이미 검색한 적이 있을 때만 즉시 재검색한다
 * (검색 전에 정렬만 바꿨는데 빈 검색어로 요청이 나가는 걸 막기 위함).
 */
function FinancialSearchForm({
  query,
  sort,
  loading,
  searched,
  onQueryChange,
  onSortChange,
  onSearch,
}) {
  const handleSubmit = (event) => {
    event.preventDefault();
    if (!loading) {
      onSearch(query, sort);
    }
  };

  const handleSortChange = (event) => {
    const nextSort = event.target.value;
    onSortChange(nextSort);
    if (searched && !loading) {
      onSearch(query, nextSort);
    }
  };

  return (
    <form className="fs-hero" onSubmit={handleSubmit}>
      <span className="fs-hero-badge">
        <DashboardIcon name="sparkle" size={13} />
        AI 금융상품 검색
      </span>

      <h2 className="fs-hero-title">어떤 금융상품을 찾으세요?</h2>
      <p className="fs-hero-subtitle">
        원하는 조건을 자연어로 입력하면 예금·적금·대출을 한 번에 찾아드려요.
      </p>

      <div className="fs-hero-input-bar">
        <span className="fs-hero-shimmer" aria-hidden="true" />
        <DashboardIcon name="search" size={18} className="fs-hero-input-icon" />
        <input
          className="fs-hero-input"
          type="text"
          value={query}
          onChange={(event) => onQueryChange(event.target.value)}
          placeholder="예) 20대가 넣기 좋은 적금"
        />
        <select
          className="fs-hero-sort"
          value={sort}
          onChange={handleSortChange}
        >
          {SORT_OPTIONS.map((option) => (
            <option key={option.value} value={option.value}>
              {option.label}
            </option>
          ))}
        </select>
        <button type="submit" className="fs-hero-send" disabled={loading}>
          {loading ? "검색 중" : "검색"}
        </button>
      </div>

      <div className="fs-hero-examples">
        <span className="fs-hero-examples-label">예시</span>
        {EXAMPLES.map((example) => (
          <button
            key={example}
            type="button"
            onClick={() => onSearch(example, sort)}
            disabled={loading}
          >
            {example}
          </button>
        ))}
      </div>
    </form>
  );
}

export default FinancialSearchForm;
