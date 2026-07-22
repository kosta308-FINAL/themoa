import SearchSettingsPanel from "./SearchSettingsPanel";
import { useSearchExplain } from "../hooks/useSearchExplain";

const EXAMPLE_QUERIES = ["청년 적금", "노후 준비", "고금리 예금", "대출"];

const PRODUCT_TYPE_INTENT_LABELS = {
  SAVINGS: "저축",
  LOAN: "대출",
};

/** 점수는 0~1 사이 실수라 소수 3자리까지만 보여준다. 값이 없으면 "-". */
const score = (value) => (value == null ? "-" : Number(value).toFixed(3));

function SearchQualityInspector() {
  const explain = useSearchExplain();
  const { result, loading } = explain;
  const interpretation = result?.interpretation;
  const candidates = result?.candidates || [];

  const handleSubmit = (event) => {
    event.preventDefault();
    explain.run();
  };

  return (
    <section className="fa-card">
      <div className="fa-card-head">
        <div>
          <h2>4. 검색 품질 점검</h2>
          <p>
            검색어가 어떻게 해석되고 어떤 상품이 몇 점으로 걸렸는지 확인합니다.
            결과가 이상할 때 원인을 숫자로 확인하는 용도예요.
          </p>
        </div>
      </div>

      {/* 설정을 바꾸면 이미 점검한 검색어로 곧바로 다시 돌려 차이를 확인할 수 있게 한다. */}
      <SearchSettingsPanel
        onSaved={() => {
          if (explain.query.trim()) {
            explain.run();
          }
        }}
      />

      <form className="fa-explain-form" onSubmit={handleSubmit}>
        <input
          type="text"
          value={explain.query}
          onChange={(event) => explain.setQuery(event.target.value)}
          placeholder="점검할 검색어 (예: 노후 준비)"
        />
        <button
          type="submit"
          className="admin-btn fa-btn-primary"
          disabled={loading}
        >
          {loading ? "점검 중…" : "점검"}
        </button>
      </form>

      <div className="fa-chips">
        {EXAMPLE_QUERIES.map((example) => (
          <button
            key={example}
            type="button"
            className="fa-chip fa-chip-neutral"
            onClick={() => explain.run(example)}
            disabled={loading}
          >
            {example}
          </button>
        ))}
      </div>

      {explain.error && (
        <div className="fa-alert fa-alert-danger">{explain.error}</div>
      )}

      {interpretation && (
        <>
          <div className="fa-explain-summary">
            <div className="fa-stat">
              <span>상품유형 의도</span>
              <strong className="fa-stat-text">
                {PRODUCT_TYPE_INTENT_LABELS[interpretation.productTypeIntent] ||
                  "감지 안 됨"}
              </strong>
            </div>
            <div className="fa-stat">
              <span>인구집단</span>
              <strong className="fa-stat-text">
                {interpretation.demographicGroups?.length
                  ? interpretation.demographicGroups.join(", ")
                  : "감지 안 됨"}
              </strong>
            </div>
            <div className="fa-stat">
              <span>나이</span>
              <strong className="fa-stat-text">
                {interpretation.age == null
                  ? "없음"
                  : `${interpretation.age}세`}
              </strong>
            </div>
            <div className="fa-stat">
              <span>벡터 검색</span>
              <strong className="fa-stat-text">
                {interpretation.vectorSearchUsed ? "사용" : "미사용"}
              </strong>
              {!interpretation.vectorSearchUsed && (
                <span className="fa-badge fa-badge-warn">꺼짐(키워드만)</span>
              )}
            </div>
            <div className="fa-stat">
              <span>벡터 히트</span>
              <strong
                className={
                  interpretation.vectorHitCount === 0 ? "fa-stat-alert" : ""
                }
              >
                {Number(interpretation.vectorHitCount ?? 0).toLocaleString()}
              </strong>
              <small>임계값 {score(interpretation.minimumSimilarity)}</small>
            </div>
          </div>

          <div className="fa-link-block">
            <h3>확장된 검색어</h3>
            {interpretation.expandedTerms?.length ? (
              <div className="fa-chips">
                {interpretation.expandedTerms.map((term) => (
                  <span key={term} className="fa-term">
                    {term}
                  </span>
                ))}
              </div>
            ) : (
              <p className="fa-note">확장된 검색어가 없어요.</p>
            )}
          </div>

          {interpretation.vectorSearchUsed &&
            interpretation.vectorHitCount === 0 && (
              <div className="fa-note fa-note-warn">
                벡터 히트가 0건이에요. 유사도 임계값(
                {score(interpretation.minimumSimilarity)})이 이 검색어에 비해
                너무 높다는 신호일 수 있어요.
              </div>
            )}

          {candidates.length === 0 ? (
            <div className="fa-note">
              후보가 없습니다 — 검색어가 상품 텍스트와 겹치지 않고, 벡터
              유사도도 임계값({score(interpretation.minimumSimilarity)})
              미만입니다.
            </div>
          ) : (
            <table className="fa-table fa-table-left">
              <thead>
                <tr>
                  <th>상품명</th>
                  <th>회사</th>
                  <th>유사도</th>
                  <th>키워드점수</th>
                  <th>최종점수</th>
                  <th>일치단어</th>
                  <th>포함</th>
                </tr>
              </thead>
              <tbody>
                {candidates.map((candidate) => (
                  <tr
                    key={`${candidate.targetType}-${candidate.productId}`}
                    className={candidate.included ? "" : "fa-row-excluded"}
                  >
                    <td>
                      {candidate.productName}
                      {!candidate.included && candidate.excludedReason && (
                        <small className="fa-excluded-reason">
                          {candidate.excludedReason}
                        </small>
                      )}
                    </td>
                    <td>{candidate.companyName}</td>
                    <td>{score(candidate.semanticScore)}</td>
                    <td>{score(candidate.lexicalScore)}</td>
                    <td className="fa-em">{score(candidate.totalScore)}</td>
                    <td>{candidate.matchedTerms?.join(", ") || "-"}</td>
                    <td>
                      <span
                        className={`fa-badge ${candidate.included ? "fa-badge-ok" : "fa-badge-fail"}`}
                      >
                        {candidate.included ? "포함" : "제외"}
                      </span>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          )}
        </>
      )}
    </section>
  );
}

export default SearchQualityInspector;
