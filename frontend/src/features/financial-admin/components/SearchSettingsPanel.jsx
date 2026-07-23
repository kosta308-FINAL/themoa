import { useSearchSettings } from "../hooks/useSearchSettings";

/** 마지막 변경 시각. 오프셋 없는 서버 시각이라 로컬로 파싱·표시해 숫자를 그대로 보존한다. */
const formatUpdatedAt = (isoString) => {
  if (!isoString) {
    return null;
  }
  const updatedAt = new Date(isoString);
  if (Number.isNaN(updatedAt.getTime())) {
    return null;
  }
  return updatedAt.toLocaleString("ko-KR", {
    month: "long",
    day: "numeric",
    hour: "2-digit",
    minute: "2-digit",
    hour12: false,
  });
};

/**
 * 검색 튜닝값 설정. 저장에 성공하면 onSaved로 알려 점검을 다시 돌리게 한다
 * (값을 바꾼 뒤 결과가 어떻게 달라지는지 바로 보기 위함).
 */
function SearchSettingsPanel({ onSaved }) {
  const settings = useSearchSettings();
  const { form, loading, saving } = settings;

  const handleSubmit = async (event) => {
    event.preventDefault();
    const saved = await settings.save();
    if (saved) {
      onSaved?.();
    }
  };

  // 저장해 둔 튜닝값이 사라지는 동작이라 실수로 누르지 않게 한 번 확인받는다.
  const handleReset = async () => {
    const confirmed = window.confirm(
      "검색 설정을 초기값으로 되돌릴까요?\n지금까지 변경한 값은 사라집니다.",
    );
    if (!confirmed) {
      return;
    }
    const done = await settings.reset();
    if (done) {
      onSaved?.();
    }
  };

  if (loading) {
    return <p className="fa-note">검색 설정을 불러오고 있어요.</p>;
  }

  const updatedAt = formatUpdatedAt(settings.settings?.updatedAt);

  return (
    <div className="fa-settings">
      <div className="fa-settings-head">
        <h3>검색 설정</h3>
        {settings.settings?.usingDefaults && (
          <span className="fa-badge fa-badge-neutral">기본값 사용 중</span>
        )}
        {updatedAt && (
          <span className="fa-settings-meta">마지막 변경: {updatedAt}</span>
        )}
      </div>

      {settings.error && (
        <div className="fa-alert fa-alert-danger">{settings.error}</div>
      )}

      <form className="fa-settings-form" onSubmit={handleSubmit}>
        <label className="fa-setting-field">
          <span>결과 개수 (topK)</span>
          <input
            type="number"
            min="1"
            max="50"
            value={form.topK}
            onChange={(event) => settings.setField("topK", event.target.value)}
          />
          <small>1 ~ 50</small>
        </label>

        <label className="fa-setting-field">
          <span>벡터 후보 개수 (retryTopK)</span>
          <input
            type="number"
            min="1"
            max="100"
            value={form.retryTopK}
            onChange={(event) =>
              settings.setField("retryTopK", event.target.value)
            }
          />
          <small>1 ~ 100</small>
        </label>

        <label className="fa-setting-field fa-setting-slider">
          <span>
            유사도 임계값
            <strong>{Number(form.minimumSimilarity || 0).toFixed(2)}</strong>
          </span>
          <input
            type="range"
            min="0"
            max="1"
            step="0.01"
            value={form.minimumSimilarity || 0}
            onChange={(event) =>
              settings.setField("minimumSimilarity", event.target.value)
            }
          />
          <small>낮출수록 의미가 느슨한 상품까지 결과에 들어와요.</small>
        </label>

        <div className="fa-setting-field fa-setting-readonly">
          <span>벡터 검색</span>
          <strong>
            {settings.settings?.vectorSearchEnabled ? "켜짐" : "꺼짐"}
          </strong>
          <small>변경하려면 설정 파일 수정 후 서버 재기동이 필요해요.</small>
        </div>

        <div className="fa-setting-actions">
          <button
            type="submit"
            className="admin-btn fa-btn-primary"
            disabled={saving || settings.resetting}
          >
            {saving ? "저장 중…" : "저장하고 다시 점검"}
          </button>
          <button
            type="button"
            className="admin-btn admin-btn-secondary"
            onClick={handleReset}
            disabled={saving || settings.resetting}
          >
            {settings.resetting ? "되돌리는 중…" : "초기값으로 되돌리기"}
          </button>
        </div>
      </form>
    </div>
  );
}

export default SearchSettingsPanel;
