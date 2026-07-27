import DashboardIcon from "../../../components/common/DashboardIcon";

function PolicySearchForm({
  query,
  examples,
  loading,
  totalText,
  onQueryChange,
  onSearch,
}) {
  const handleKeyDown = (event) => {
    if (event.key === "Enter" && !event.shiftKey) {
      event.preventDefault();
      if (!loading && query.trim()) onSearch();
    }
  };

  return (
    <section className="policy-chat-hero">
      <span className="policy-chat-badge">
        <DashboardIcon name="sparkle" size={13} />
        AI 정책 검색
      </span>

      <h2 className="policy-chat-title">어떤 정책이 궁금하신가요?</h2>
      <p className="policy-chat-subtitle">
        지역, 나이, 학생 여부와 취업 상태를 문장으로 입력하면 조건에 맞는 정책을
        찾아드려요.
      </p>

      <div className="policy-chat-input-bar">
        <DashboardIcon
          name="search"
          size={18}
          className="policy-chat-input-icon"
        />
        <textarea
          value={query}
          onChange={(event) => onQueryChange(event.target.value)}
          onKeyDown={handleKeyDown}
          rows={1}
          placeholder="예: 수원에 사는 27살 취업 준비생이 받을 수 있는 정책"
        />
        <button
          type="button"
          className="policy-chat-send"
          disabled={loading || !query.trim()}
          onClick={() => onSearch()}
        >
          {loading ? "검색 중" : "정책 찾기"}
        </button>
      </div>

      <div className="policy-chat-examples">
        <span className="policy-chat-examples-label">예시</span>
        {examples.map((example) => (
          <button
            key={example}
            type="button"
            onClick={() => onQueryChange(example)}
          >
            {example}
          </button>
        ))}
      </div>

      <span className="policy-chat-status">
        {loading ? "검색 중…" : totalText}
      </span>
    </section>
  );
}

export default PolicySearchForm;
