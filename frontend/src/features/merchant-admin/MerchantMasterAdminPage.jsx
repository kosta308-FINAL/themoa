import { useEffect, useMemo, useState } from "react";
import AdminLayout from "../../components/layout/AdminLayout";
import DashboardIcon from "../../components/common/DashboardIcon";
import {
  getPromotionCandidates,
  promoteMerchantAliasTerm,
  promoteMerchantAliasTermAsNewService,
  rejectMerchantAliasProposal,
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

/** 카드사가 준 원본 표기(aliasText)를 기준으로 묶는다 — 확정된 사실은 이 문자열이고, 서비스명은
 * 회원이 그 문자열에 붙인 "제안"일 뿐이다. 같은 원본 표기에 서로 다른 서비스명이 제안돼 있으면
 * (예: 같은 "tving_subscription"을 누구는 "TVING 구독", 누구는 "tving 구독"으로 등록) 그 자체가
 * 서비스가 중복 생성됐다는 신호라서, 카드 안에 제안된 서비스명 전부를 나란히 보여준다. */
function groupCandidatesByRawText(candidates) {
  const groups = new Map();
  candidates.forEach((candidate) => {
    const key = candidate.aliasText;
    if (!groups.has(key)) {
      groups.set(key, {
        aliasText: candidate.aliasText,
        proposals: [],
      });
    }
    groups.get(key).proposals.push({
      aliasId: candidate.aliasId,
      canonicalServiceName: candidate.canonicalServiceName,
      categoryName: candidate.categoryName,
      learnerCount: candidate.learnerCount,
    });
  });
  return Array.from(groups.values())
    .map((group) => ({
      ...group,
      learnerTotal: group.proposals.reduce((sum, p) => sum + p.learnerCount, 0),
      hasConflict: group.proposals.length > 1,
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
  const [quickForm, setQuickForm] = useState({});
  const [registeringId, setRegisteringId] = useState(null);
  const [newServiceForm, setNewServiceForm] = useState({});

  const candidateGroups = useMemo(
    () => groupCandidatesByRawText(candidates),
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

  /** 원본 표기 하나를, 그 표기에 제안된 서비스명 하나로 전역 기본값으로 승격한다. 같은 표기에 제안이
   * 여러 개(중복 alias)여도 다른 제안·다른 학습자는 절대 건드리지 않는다 — resolve()가 "내 학습 →
   * 전역" 순으로 찾기 때문에, 이미 자기 이름으로 학습해 둔 사람은 승격 이후에도 계속 자기 것을 본다.
   * 전역 승격은 그 표기를 한 번도 학습한 적 없는 새 회원을 위한 기본값일 뿐이다. */
  const handlePromoteProposal = async (group, proposal) => {
    const key = `${group.aliasText}:${proposal.aliasId}`;
    setPromotingKey(key);
    setError("");
    try {
      await promoteMerchantAliasTerm(proposal.aliasId, group.aliasText);
      await load();
    } catch (requestError) {
      setError(getApiErrorMessage(requestError, "승격에 실패했어요."));
    } finally {
      setPromotingKey(null);
    }
  };

  /** 이 (표기, 제안 서비스명) 조합이 틀렸다고 판단해 대기목록에서 다시 안 뜨게 반려한다. 학습한
   * 회원의 개인 표기는 그대로 두므로, 그 회원 본인 화면에는 영향이 없다. */
  const handleRejectProposal = async (group, proposal) => {
    const key = `${group.aliasText}:${proposal.aliasId}`;
    setPromotingKey(key);
    setError("");
    try {
      await rejectMerchantAliasProposal(proposal.aliasId, group.aliasText);
      await load();
    } catch (requestError) {
      setError(getApiErrorMessage(requestError, "반려에 실패했어요."));
    } finally {
      setPromotingKey(null);
    }
  };

  const updateNewServiceForm = (aliasText, patch) => {
    setNewServiceForm((prev) => ({
      ...prev,
      [aliasText]: { ...prev[aliasText], ...patch },
    }));
  };

  /** 제안된 이름이 다 마땅치 않을 때, 관리자가 새 서비스명을 직접 지어서 이 표기를 그걸로 승격한다.
   * 기존 제안 중 하나를 고르는 것과 마찬가지로 다른 학습자의 개인 표기는 건드리지 않는다. */
  const handlePromoteAsNewService = async (group) => {
    const form = newServiceForm[group.aliasText] || {};
    const name = (form.name || "").trim();
    if (!name) {
      setError("새 서비스명을 입력해 주세요.");
      return;
    }
    const key = `${group.aliasText}:new`;
    setPromotingKey(key);
    setError("");
    try {
      await promoteMerchantAliasTermAsNewService(
        group.aliasText,
        name,
        form.categoryId || undefined,
      );
      setNewServiceForm((prev) => ({ ...prev, [group.aliasText]: undefined }));
      await load();
    } catch (requestError) {
      setError(getApiErrorMessage(requestError, "승격에 실패했어요."));
    } finally {
      setPromotingKey(null);
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
      subtitle="데이터 수집·활용에 동의한 회원의 학습 표기·거래만 모아 전역 승격과 미식별 가맹점 등록을 관리합니다"
    >
      <div className="mma-page">
        <section className="mma-kpi-grid">
          <div className="mma-kpi-card">
            <span className="mma-kpi-icon purple">
              <DashboardIcon name="sparkle" size={18} />
            </span>
            <div className="mma-kpi-body">
              <span className="mma-kpi-title">전역 승격 대기 표기</span>
              <span className="mma-kpi-value">{candidateGroups.length} 건</span>
              <span className="mma-kpi-hint">
                서비스명 제안 {candidates.length}건
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
                  원본 표기별로 회원들이 제안한 서비스명을 모아 보여줍니다.
                  승격은 새 회원의 기본값만 정하며, 기존 학습 회원에게는
                  영향을 주지 않습니다.
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
                <div className="mma-group-card" key={group.aliasText}>
                  <div className="mma-group-header">
                    <div className="mma-group-heading">
                      <code className="mma-rawtext-badge">
                        {group.aliasText}
                      </code>
                      <span className="mma-group-meta">
                        제안된 서비스명 {group.proposals.length}개 · 학습 총{" "}
                        {group.learnerTotal}명
                      </span>
                      {group.hasConflict && (
                        <span className="mma-badge purple">제안 여러 개</span>
                      )}
                    </div>
                  </div>
                  <ul className="mma-variant-list">
                    {group.proposals.map((proposal) => {
                      const key = `${group.aliasText}:${proposal.aliasId}`;
                      return (
                        <li className="mma-variant-row" key={key}>
                          <span className="mma-badge purple">
                            {proposal.canonicalServiceName}
                          </span>
                          <span className="mma-variant-category">
                            {proposal.categoryName || "카테고리 미지정"}
                          </span>
                          <span className="mma-variant-learners">
                            {proposal.learnerCount}명 학습
                          </span>
                          <button
                            type="button"
                            className="mma-btn mma-btn-ghost-red mma-btn-sm"
                            disabled={promotingKey === key}
                            onClick={() =>
                              handleRejectProposal(group, proposal)
                            }
                          >
                            반려
                          </button>
                          <button
                            type="button"
                            className="mma-btn mma-btn-primary mma-btn-sm"
                            disabled={promotingKey === key}
                            onClick={() =>
                              handlePromoteProposal(group, proposal)
                            }
                          >
                            {promotingKey === key
                              ? "승격 중..."
                              : "이 이름으로 승격"}
                          </button>
                        </li>
                      );
                    })}
                  </ul>
                  <div className="mma-quick-form">
                    <input
                      type="text"
                      className="mma-input"
                      placeholder="제안된 이름이 마땅치 않다면 새 서비스명을 입력하세요"
                      value={newServiceForm[group.aliasText]?.name || ""}
                      onChange={(e) =>
                        updateNewServiceForm(group.aliasText, {
                          name: e.target.value,
                        })
                      }
                    />
                    <select
                      className="mma-select"
                      value={newServiceForm[group.aliasText]?.categoryId || ""}
                      onChange={(e) =>
                        updateNewServiceForm(group.aliasText, {
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
                      disabled={promotingKey === `${group.aliasText}:new`}
                      onClick={() => handlePromoteAsNewService(group)}
                    >
                      {promotingKey === `${group.aliasText}:new`
                        ? "승격 중..."
                        : "새 이름으로 승격"}
                    </button>
                  </div>
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
                  최근 15일간 전역 alias가 없는 상위 원본 가맹점 리스트입니다
                  (데이터 수집·활용에 동의한 회원의 거래만 집계). 바로
                  마스터에 등록하세요.
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
