import { useState } from "react";

/**
 * 키워드 그룹 하나. 추가 입력창이 그룹 안에 있어서 groupKey를 직접 입력할 일이 없다
 * — 오타로 엉뚱한 그룹이 새로 만들어지는 것을 구조적으로 막기 위함.
 */
function KeywordGroupCard({
  group,
  groupLabel,
  keywordType,
  busy,
  onAdd,
  onRemove,
}) {
  const [keyword, setKeyword] = useState("");

  const adding = busy === `add:${group.groupKey}`;

  const handleSubmit = async (event) => {
    event.preventDefault();
    const added = await onAdd(keywordType, group.groupKey, keyword);
    if (added) {
      setKeyword("");
    }
  };

  return (
    <div className="fa-keyword-group">
      <div className="fa-keyword-group-head">
        <strong>{group.groupKey}</strong>
        {groupLabel && <span className="fa-keyword-label">{groupLabel}</span>}
        <span className="fa-count">{group.keywords?.length || 0}</span>
      </div>

      <div className="fa-chips">
        {group.keywords?.length ? (
          group.keywords.map((item) => (
            <span key={item.id} className="fa-keyword-chip">
              {item.keyword}
              <button
                type="button"
                aria-label={`${item.keyword} 삭제`}
                disabled={busy === `remove:${item.id}`}
                onClick={() => onRemove(item.id)}
              >
                ×
              </button>
            </span>
          ))
        ) : (
          <span className="fa-note">등록된 키워드가 없어요.</span>
        )}
      </div>

      <form className="fa-keyword-add" onSubmit={handleSubmit}>
        <input
          type="text"
          value={keyword}
          onChange={(event) => setKeyword(event.target.value)}
          placeholder="키워드 추가 (예: 노후)"
        />
        <button
          type="submit"
          className="admin-btn admin-btn-secondary"
          disabled={adding}
        >
          {adding ? "추가 중…" : "+ 추가"}
        </button>
      </form>
    </div>
  );
}

export default KeywordGroupCard;
