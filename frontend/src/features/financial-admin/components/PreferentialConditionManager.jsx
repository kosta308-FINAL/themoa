import { useState } from "react";
import { usePreferentialConditions } from "../hooks/usePreferentialConditions";
import "./PreferentialConditionManager.css";

const formatDateTime = (isoString) => {
  if (!isoString) {
    return "";
  }
  const date = new Date(isoString);
  if (Number.isNaN(date.getTime())) {
    return "";
  }
  return date.toLocaleString("ko-KR", {
    year: "numeric",
    month: "2-digit",
    day: "2-digit",
    hour: "2-digit",
    minute: "2-digit",
    hour12: false,
  });
};

/** 상태 뱃지: 잠금(사람이 수정함) / 재검토 필요(원문 바뀜). */
function StatusBadges({ cache }) {
  return (
    <span className="pc-badges">
      {cache.editedByAdmin && (
        <span className="pc-badge pc-badge-lock">🔒 관리자 잠금</span>
      )}
      {cache.stale && (
        <span className="pc-badge pc-badge-stale">⚠️ 재검토 필요</span>
      )}
    </span>
  );
}

/**
 * 우대조건 파싱 캐시 관리 섹션. 잘못 파싱된 상품을 교정·잠금하고,
 * 원문 변경으로 재검토가 필요한(stale) 상품을 확인한다.
 * 상품 목록과 연동이 없어 productId 직접 입력으로 조회한다.
 */
function PreferentialConditionManager() {
  const conditions = usePreferentialConditions();
  const [productIdInput, setProductIdInput] = useState("");
  const [editItems, setEditItems] = useState(null);
  const [saved, setSaved] = useState(false);

  // 항목 배열을 편집용 복사본으로 옮긴다(원본을 직접 건드리지 않도록).
  const fillEditor = (items) => {
    setSaved(false);
    setEditItems(
      (items || []).map((item) => ({
        description: item.description || "",
        rateBonus: item.rateBonus ?? 0,
      })),
    );
  };

  const handleLookup = async (event) => {
    event?.preventDefault();
    const id = productIdInput.trim();
    if (!id) {
      return;
    }
    const cache = await conditions.lookup(id);
    if (cache) {
      fillEditor(cache.items);
    } else {
      setEditItems(null);
    }
  };

  const handleReviewEdit = async (productId) => {
    setProductIdInput(String(productId));
    const cache = await conditions.lookup(productId);
    if (cache) {
      fillEditor(cache.items);
    }
  };

  // 재검토 목록에서: 상세(뱃지·시각)를 불러오면서 최신 원문 재파싱 초안을 편집기에 채운다.
  const handleReparse = async (productId) => {
    setProductIdInput(String(productId));
    await conditions.lookup(productId);
    const items = await conditions.reparse(productId);
    if (items) {
      fillEditor(items);
    }
  };

  // 편집기에서: 현재 조회한 상품을 최신 원문으로 다시 파싱해 항목을 교체한다.
  const handleReparseCurrent = async () => {
    const items = await conditions.reparse(conditions.detail.productId);
    if (items) {
      fillEditor(items);
    }
  };

  const updateItem = (index, patch) => {
    setEditItems((prev) =>
      prev.map((item, i) => (i === index ? { ...item, ...patch } : item)),
    );
  };

  const addItem = () => {
    setEditItems((prev) => [
      ...(prev || []),
      { description: "", rateBonus: 0 },
    ]);
  };

  const removeItem = (index) => {
    setEditItems((prev) => prev.filter((_, i) => i !== index));
  };

  const handleSave = async () => {
    const items = editItems.map((item) => ({
      description: item.description.trim(),
      rateBonus: Number(item.rateBonus) || 0,
    }));
    const ok = await conditions.save(conditions.detail.productId, items);
    if (ok) {
      setSaved(true);
    }
  };

  return (
    <>
      <section className="fa-card" id="conditions">
        <div className="fa-card-head">
          <div>
            <h2>우대조건 파싱 관리</h2>
            <p>
              예·적금 우대조건을 LLM이 체크리스트로 파싱한 결과를 관리합니다.
              재생성은 판매중 상품 전체를 다시 파싱해 시간이 걸립니다.
            </p>
          </div>
          <button
            type="button"
            className="admin-btn fa-btn-primary"
            onClick={conditions.runRefresh}
            disabled={conditions.refreshing}
          >
            {conditions.refreshing ? "재생성 중…" : "캐시 재생성"}
          </button>
        </div>

        {conditions.error && (
          <div className="fa-alert fa-alert-danger">{conditions.error}</div>
        )}

        {conditions.refreshing && (
          <div className="fa-alert fa-alert-info">
            전체 상품을 LLM으로 다시 파싱하고 있어요. 수 분 걸릴 수 있으니 이
            화면을 닫지 말아 주세요.
          </div>
        )}

        {conditions.refreshResult && (
          <div className="fa-note fa-note-done">
            재생성 완료 — 총{" "}
            {Number(conditions.refreshResult.total ?? 0).toLocaleString()}건
            처리, 실패{" "}
            {Number(conditions.refreshResult.failed ?? 0).toLocaleString()}
            건.
          </div>
        )}

        <div className="pc-block">
          <h3>
            재검토 필요
            <span className="fa-count">{conditions.reviewItems.length}</span>
          </h3>

          {conditions.reviewLoading ? (
            <p className="fa-note">재검토 목록을 불러오고 있어요.</p>
          ) : conditions.reviewItems.length === 0 ? (
            <p className="fa-note">재검토가 필요한 상품이 없어요.</p>
          ) : (
            <ul className="pc-review-list">
              {conditions.reviewItems.map((cache) => (
                <li className="pc-review-item" key={cache.productId}>
                  <div className="pc-review-head">
                    <div>
                      <strong>상품 #{cache.productId}</strong>
                      <StatusBadges cache={cache} />
                    </div>
                    <div className="pc-review-actions">
                      <span className="pc-time">
                        {formatDateTime(cache.updatedAt)}
                      </span>
                      <button
                        type="button"
                        className="admin-btn fa-btn-primary"
                        disabled={conditions.reparsing}
                        onClick={() => handleReparse(cache.productId)}
                      >
                        {conditions.reparsing
                          ? "재파싱 중…"
                          : "최신 원문 재파싱"}
                      </button>
                      <button
                        type="button"
                        className="admin-btn admin-btn-secondary"
                        onClick={() => handleReviewEdit(cache.productId)}
                      >
                        수정
                      </button>
                    </div>
                  </div>
                  {cache.items?.length > 0 ? (
                    <ul className="pc-item-preview">
                      {cache.items.map((item, index) => (
                        <li key={index}>
                          {item.description}
                          <em>+{item.rateBonus}%p</em>
                        </li>
                      ))}
                    </ul>
                  ) : (
                    <p className="fa-note">파싱된 우대조건이 없어요.</p>
                  )}
                </li>
              ))}
            </ul>
          )}
        </div>
      </section>

      <section className="fa-card">
        <div className="fa-card-head">
          <div>
            <h2>상품별 우대조건 수정</h2>
            <p>
              상품 ID로 캐시를 조회해 항목을 직접 고치고 저장합니다. 저장하면
              잠금 처리되어 이후 배치가 덮어쓰지 않습니다.
            </p>
          </div>
        </div>

        <form className="pc-lookup" onSubmit={handleLookup}>
          <input
            type="number"
            min="1"
            value={productIdInput}
            onChange={(event) => setProductIdInput(event.target.value)}
            placeholder="상품 ID (예: 123)"
          />
          <button
            type="submit"
            className="admin-btn fa-btn-primary"
            disabled={conditions.lookupBusy}
          >
            {conditions.lookupBusy ? "조회 중…" : "조회"}
          </button>
        </form>

        {conditions.lookupError && (
          <div className="fa-alert fa-alert-danger">
            {conditions.lookupError}
          </div>
        )}

        {conditions.detail && editItems && (
          <div className="pc-editor">
            <div className="pc-editor-head">
              <strong>상품 #{conditions.detail.productId}</strong>
              <StatusBadges cache={conditions.detail} />
              <span className="pc-time">
                {formatDateTime(conditions.detail.updatedAt)}
              </span>
              <button
                type="button"
                className="admin-btn admin-btn-secondary pc-reparse"
                disabled={conditions.reparsing}
                onClick={handleReparseCurrent}
              >
                {conditions.reparsing ? "재파싱 중…" : "최신 원문으로 재파싱"}
              </button>
            </div>

            {conditions.reparsing && (
              <div className="fa-alert fa-alert-info">
                최신 원문을 LLM으로 다시 파싱하고 있어요. 잠시만 기다려 주세요.
              </div>
            )}

            {saved && (
              <div className="fa-note fa-note-done">
                저장했어요. 이 상품은 이제 관리자 잠금 상태입니다.
              </div>
            )}

            <div className="pc-edit-rows">
              {editItems.length === 0 && (
                <p className="fa-note">항목이 없어요. 아래에서 추가하세요.</p>
              )}
              {editItems.map((item, index) => (
                <div className="pc-edit-row" key={index}>
                  <input
                    type="text"
                    className="pc-edit-desc"
                    value={item.description}
                    placeholder="조건 설명"
                    onChange={(event) =>
                      updateItem(index, { description: event.target.value })
                    }
                  />
                  <div className="pc-edit-bonus">
                    <span>+</span>
                    <input
                      type="number"
                      step="0.01"
                      min="0"
                      value={item.rateBonus}
                      onChange={(event) =>
                        updateItem(index, { rateBonus: event.target.value })
                      }
                    />
                    <em>%p</em>
                  </div>
                  <button
                    type="button"
                    className="pc-edit-remove"
                    onClick={() => removeItem(index)}
                    aria-label="항목 삭제"
                  >
                    ×
                  </button>
                </div>
              ))}
            </div>

            <div className="pc-editor-foot">
              <button
                type="button"
                className="admin-btn admin-btn-secondary"
                onClick={addItem}
              >
                + 항목 추가
              </button>
              <button
                type="button"
                className="admin-btn fa-btn-primary"
                onClick={handleSave}
                disabled={conditions.saving}
              >
                {conditions.saving ? "저장 중…" : "저장 (잠금)"}
              </button>
            </div>
          </div>
        )}
      </section>
    </>
  );
}

export default PreferentialConditionManager;
