import { useEffect, useMemo, useState } from "react";
import AdminLayout from "../../components/layout/AdminLayout";
import DashboardIcon from "../../components/common/DashboardIcon";
import {
  getPromotionCandidates,
  promoteMerchantAliasTerm,
  getUnclassifiedMerchants,
  registerQuickMerchantAlias,
} from "../../api/merchantAliasApi";
import { getCategories } from "../../api/spendingGuideApi";
import { getApiErrorMessage } from "../../utils/apiError";
import "./MerchantMasterAdminPage.css";

function formatAmount(value) {
  const num = Number(value);
  if (!Number.isFinite(num)) return "-";
  return `${Math.round(num).toLocaleString()}원`;
}

/** 같은 canonicalServiceName(연결될 서비스)으로 들어온 여러 원본 표기(variant)를 한 그룹으로 묶는다.
 * "chat-g-p-t" / "ChatGPT구독" / "ai구독"처럼 회원마다 제각각 학습한 표기라도 결국 같은 서비스로
 * 승격될 후보이므로, 관리자 화면에서는 서비스 단위 카드 안에 표기 변형들을 나열해 한눈에 보이게 한다. */
function groupCandidatesByService(candidates) {
  const groups = new Map();
  candidates.forEach((candidate) => {
    const key = candidate.aliasId;
    if (!groups.has(key)) {
      groups.set(key, {
        aliasId: candidate.aliasId,
        canonicalServiceName: candidate.canonicalServiceName,
        categoryName: candidate.categoryName,
        variants: [],
      });
    }
    groups.get(key).variants.push({
      aliasText: candidate.aliasText,
      learnerCount: candidate.learnerCount,
    });
  });
  return Array.from(groups.values())
    .map((group) => ({
      ...group,
      learnerTotal: group.variants.reduce((sum, v) => sum + v.learnerCount, 0),
    }))
    .sort((a, b) => b.learnerTotal - a.learnerTotal);
}

function MerchantMasterAdminPage() {
  const [candidates, setCandidates] = useState([]);
  const [unclassified, setUnclassified] = useState([]);
  const [categories, setCategories] = useState([]);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState("");
  const [promotingKey, setPromotingKey] = useState(null);
  const [bulkPromotingId, setBulkPromotingId] = useState(null);
  const [quickForm, setQuickForm] = useState({});
  const [registeringId, setRegisteringId] = useState(null);

  const candidateGroups = useMemo(
    () => groupCandidatesByService(candidates),
    [candidates],
  );

  const load = async () => {
    setIsLoading(true);
    setError("");
    try {
      const [candidateData, unclassifiedData] = await Promise.all([
        getPromotionCandidates(),
        getUnclassifiedMerchants(),
      ]);
      setCandidates(candidateData || []);
      setUnclassified(unclassifiedData || []);
    } catch (requestError) {
      setError(
        getApiErrorMessage(
          requestError,
          "가맹점 마스터 데이터를 불러오지 못했어요.",
        ),
      );
    } finally {
      setIsLoading(false);
    }
  };

  useEffect(() => {
    const timer = window.setTimeout(load, 0);
    return () => window.clearTimeout(timer);
  }, []);

  useEffect(() => {
    getCategories()
      .then((data) => setCategories(data || []))
      .catch(() => {});
  }, []);

  const handlePromote = async (candidate) => {
    const key = `${candidate.aliasId}:${candidate.aliasText}`;
    setPromotingKey(key);
    setError("");
    try {
      await promoteMerchantAliasTerm(candidate.aliasId, candidate.aliasText);
      await load();
    } catch (requestError) {
      setError(getApiErrorMessage(requestError, "승격에 실패했어요."));
    } finally {
      setPromotingKey(null);
    }
  };

  const handlePromoteGroup = async (group) => {
    setBulkPromotingId(group.aliasId);
    setError("");
    try {
      for (const variant of group.variants) {
        await promoteMerchantAliasTerm(group.aliasId, variant.aliasText);
      }
      await load();
    } catch (requestError) {
      setError(getApiErrorMessage(requestError, "일괄 승격에 실패했어요."));
    } finally {
      setBulkPromotingId(null);
    }
  };

  const updateQuickForm = (merchantId, patch) => {
    setQuickForm((prev) => ({
      ...prev,
      [merchantId]: { ...prev[merchantId], ...patch },
    }));
  };

  const handleQuickRegister = async (merchant) => {
    const form = quickForm[merchant.merchantId] || {};
    const name = (form.name ?? merchant.merchantNameRaw).trim();
    if (!name) {
      setError("서비스명을 입력해 주세요.");
      return;
    }
    setRegisteringId(merchant.merchantId);
    setError("");
    try {
      await registerQuickMerchantAlias(
        merchant.merchantId,
        name,
        form.categoryId || undefined,
      );
      await load();
    } catch (requestError) {
      setError(getApiErrorMessage(requestError, "마스터 등록에 실패했어요."));
    } finally {
      setRegisteringId(null);
    }
  };

  return (
    <AdminLayout
      title="가맹점 & 서비스 마스터 관리"
      subtitle="회원 학습 표기의 전역 승격과 미식별 가맹점 등록을 관리합니다"
    >
      <div className="mma-page">
        <section className="mma-kpi-grid">
          <div className="mma-kpi-card">
            <span className="mma-kpi-icon purple">
              <DashboardIcon name="sparkle" size={18} />
            </span>
            <div className="mma-kpi-body">
              <span className="mma-kpi-title">전역 승격 대기 서비스</span>
              <span className="mma-kpi-value">{candidateGroups.length} 개</span>
              <span className="mma-kpi-hint">
                표기 {candidates.length}건 발견
              </span>
            </div>
          </div>
          <div className="mma-kpi-card">
            <span className="mma-kpi-icon amber">
              <DashboardIcon name="search" size={18} />
            </span>
            <div className="mma-kpi-body">
              <span className="mma-kpi-title">최근 15일 미식별 가맹점</span>
              <span className="mma-kpi-value warn">
                {unclassified.length} 건
              </span>
            </div>
          </div>
        </section>

        {error && <div className="mma-alert">{error}</div>}

        <section className="mma-panel">
          <div className="mma-panel-header">
            <div className="mma-panel-heading">
              <span className="mma-panel-icon purple">
                <DashboardIcon name="sparkle" size={18} />
              </span>
              <div>
                <div className="mma-panel-title">전역 마스터 승격 대기목록</div>
                <div className="mma-panel-sub">
                  같은 서비스를 가리키는 여러 원본 표기를 회원들이 각자 다르게
                  학습한 경우, 서비스 단위로 묶어 보여줍니다. 표기별로 개별
                  승격하거나 한 번에 전체 승격할 수 있습니다.
                </div>
              </div>
            </div>
          </div>
          {isLoading ? (
            <div className="mma-empty">불러오는 중...</div>
          ) : candidateGroups.length === 0 ? (
            <div className="mma-empty">승격 대기 중인 표기가 없습니다.</div>
          ) : (
            <div className="mma-group-list">
              {candidateGroups.map((group) => (
                <div className="mma-group-card" key={group.aliasId}>
                  <div className="mma-group-header">
                    <div className="mma-group-heading">
                      <span className="mma-badge purple">
                        {group.canonicalServiceName}
                      </span>
                      <span className="mma-group-meta">
                        표기 {group.variants.length}종 · 학습 총{" "}
                        {group.learnerTotal}명
                      </span>
                      <span className="mma-group-category">
                        {group.categoryName || "카테고리 미지정"}
                      </span>
                    </div>
                    {group.variants.length > 1 && (
                      <button
                        type="button"
                        className="mma-btn mma-btn-outline mma-btn-sm"
                        disabled={bulkPromotingId === group.aliasId}
                        onClick={() => handlePromoteGroup(group)}
                      >
                        {bulkPromotingId === group.aliasId
                          ? "전체 승격 중..."
                          : `표기 ${group.variants.length}종 전체 승격`}
                      </button>
                    )}
                  </div>
                  <ul className="mma-variant-list">
                    {group.variants.map((variant) => {
                      const key = `${group.aliasId}:${variant.aliasText}`;
                      return (
                        <li className="mma-variant-row" key={key}>
                          <code>{variant.aliasText}</code>
                          <span className="mma-variant-learners">
                            {variant.learnerCount}명 학습
                          </span>
                          <button
                            type="button"
                            className="mma-btn mma-btn-primary mma-btn-sm"
                            disabled={
                              promotingKey === key ||
                              bulkPromotingId === group.aliasId
                            }
                            onClick={() =>
                              handlePromote({
                                aliasId: group.aliasId,
                                aliasText: variant.aliasText,
                              })
                            }
                          >
                            {promotingKey === key ? "승격 중..." : "승격"}
                          </button>
                        </li>
                      );
                    })}
                  </ul>
                </div>
              ))}
            </div>
          )}
        </section>

        <section className="mma-panel">
          <div className="mma-panel-header">
            <div className="mma-panel-heading">
              <span className="mma-panel-icon amber">
                <DashboardIcon name="search" size={18} />
              </span>
              <div>
                <div className="mma-panel-title">
                  미식별 &amp; &apos;기타&apos; 가맹점 작업대
                </div>
                <div className="mma-panel-sub">
                  최근 15일간 전역 alias가 없는 상위 원본 가맹점 리스트입니다.
                  바로 마스터에 등록하세요.
                </div>
              </div>
            </div>
          </div>
          {isLoading ? (
            <div className="mma-empty">불러오는 중...</div>
          ) : unclassified.length === 0 ? (
            <div className="mma-empty">미식별 가맹점이 없습니다.</div>
          ) : (
            <table className="mma-table">
              <thead>
                <tr>
                  <th>발생 건수</th>
                  <th>카드사 원본 가맹점명</th>
                  <th>카드사 업종</th>
                  <th>평균 결제 금액</th>
                  <th>빠른 마스터 매핑 폼</th>
                </tr>
              </thead>
              <tbody>
                {unclassified.map((merchant) => {
                  const form = quickForm[merchant.merchantId] || {};
                  const isRegistering = registeringId === merchant.merchantId;
                  return (
                    <tr key={merchant.merchantId}>
                      <td>
                        <strong>{merchant.transactionCount} 건</strong>
                      </td>
                      <td>
                        <code>{merchant.merchantNameRaw}</code>
                      </td>
                      <td>{merchant.merchantTypeRaw || "-"}</td>
                      <td>{formatAmount(merchant.averageAmount)}</td>
                      <td>
                        <div className="mma-quick-form">
                          <input
                            type="text"
                            className="mma-input"
                            placeholder="서비스명"
                            value={form.name ?? merchant.merchantNameRaw}
                            onChange={(e) =>
                              updateQuickForm(merchant.merchantId, {
                                name: e.target.value,
                              })
                            }
                          />
                          <select
                            className="mma-select"
                            value={form.categoryId || ""}
                            onChange={(e) =>
                              updateQuickForm(merchant.merchantId, {
                                categoryId: e.target.value,
                              })
                            }
                          >
                            <option value="">카테고리</option>
                            {categories.map((category) => (
                              <option key={category.id} value={category.id}>
                                {category.name}
                              </option>
                            ))}
                          </select>
                          <button
                            type="button"
                            className="mma-btn mma-btn-primary mma-btn-sm"
                            disabled={isRegistering}
                            onClick={() => handleQuickRegister(merchant)}
                          >
                            {isRegistering ? "등록 중..." : "등록"}
                          </button>
                        </div>
                      </td>
                    </tr>
                  );
                })}
              </tbody>
            </table>
          )}
        </section>
      </div>
    </AdminLayout>
  );
}

export default MerchantMasterAdminPage;
