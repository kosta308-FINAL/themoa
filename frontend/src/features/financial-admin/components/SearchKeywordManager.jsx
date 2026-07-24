import KeywordGroupCard from "./KeywordGroupCard";
import { useSearchKeywords } from "../hooks/useSearchKeywords";

/** 그룹키만으론 뜻을 알기 어려워 한글 설명을 함께 보여준다. 모르는 키는 그대로 노출한다. */
const GROUP_LABELS = {
  YOUTH: "청년",
  SENIOR: "시니어",
  CHILDCARE: "육아",
  SAVINGS: "저축",
  LOAN: "대출",
};

/** 상품의도는 저축·대출 두 갈래뿐이라, 서버에 없더라도 이 순서로 항상 노출한다. */
const PRODUCT_INTENT_KEYS = ["SAVINGS", "LOAN"];

const emptyGroup = (groupKey) => ({ groupKey, keywords: [] });

function SearchKeywordManager() {
  const keywords = useSearchKeywords();

  // 상품의도는 SAVINGS/LOAN만 다루므로, 서버 응답에 없으면 빈 그룹으로 채워 선택지를 고정한다.
  const productIntentGroups = PRODUCT_INTENT_KEYS.map(
    (key) =>
      keywords.productIntents.find((group) => group.groupKey === key) ||
      emptyGroup(key),
  );

  const handleReset = async () => {
    const confirmed = window.confirm(
      "검색 키워드를 기본값으로 초기화할까요?\n지금까지 추가·삭제한 내용이 전부 사라집니다.",
    );
    if (confirmed) {
      await keywords.reset();
    }
  };

  return (
    <section className="fa-card">
      <div className="fa-card-head">
        <div>
          <h2>5. 검색 키워드 관리</h2>
          <p>
            검색어를 해석하는 동의어와 상품유형 의도 키워드를 편집합니다. 검색이
            특정 단어를 못 알아들을 때 해당 그룹에 추가하면 됩니다.
          </p>
        </div>
        <button
          type="button"
          className="admin-btn admin-btn-secondary"
          onClick={handleReset}
          disabled={keywords.busy === "reset"}
        >
          {keywords.busy === "reset" ? "초기화 중…" : "기본값으로 초기화"}
        </button>
      </div>

      <div className="fa-note fa-note-action">
        변경하면 검색에 즉시 반영됩니다. 위 &quot;검색 품질 점검&quot;에서
        확인해 보세요.
      </div>

      {keywords.error && (
        <div className="fa-alert fa-alert-danger">{keywords.error}</div>
      )}

      {keywords.loading ? (
        <p className="fa-note">키워드를 불러오고 있어요.</p>
      ) : (
        <>
          <div className="fa-link-block">
            <h3>
              인구집단 동의어
              <span className="fa-count">
                {keywords.demographicGroups.length}
              </span>
            </h3>
            {keywords.demographicGroups.length === 0 ? (
              <p className="fa-note">등록된 그룹이 없어요.</p>
            ) : (
              <div className="fa-keyword-grid">
                {keywords.demographicGroups.map((group) => (
                  <KeywordGroupCard
                    key={group.groupKey}
                    group={group}
                    groupLabel={GROUP_LABELS[group.groupKey]}
                    keywordType="DEMOGRAPHIC"
                    busy={keywords.busy}
                    onAdd={keywords.add}
                    onRemove={keywords.remove}
                  />
                ))}
              </div>
            )}
          </div>

          <div className="fa-link-block">
            <h3>
              상품유형 의도 키워드
              <span className="fa-count">{productIntentGroups.length}</span>
            </h3>
            <div className="fa-keyword-grid">
              {productIntentGroups.map((group) => (
                <KeywordGroupCard
                  key={group.groupKey}
                  group={group}
                  groupLabel={GROUP_LABELS[group.groupKey]}
                  keywordType="PRODUCT_INTENT"
                  busy={keywords.busy}
                  onAdd={keywords.add}
                  onRemove={keywords.remove}
                />
              ))}
            </div>
          </div>
        </>
      )}
    </section>
  );
}

export default SearchKeywordManager;
