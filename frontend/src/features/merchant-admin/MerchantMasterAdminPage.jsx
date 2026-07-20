import { useEffect, useState } from "react";
import AdminLayout from "../../components/layout/AdminLayout";
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

function MerchantMasterAdminPage() {
  const [candidates, setCandidates] = useState([]);
  const [unclassified, setUnclassified] = useState([]);
  const [categories, setCategories] = useState([]);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState("");
  const [promotingKey, setPromotingKey] = useState(null);
  const [quickForm, setQuickForm] = useState({});
  const [registeringId, setRegisteringId] = useState(null);

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
            <span className="mma-kpi-title">전역 승격 대기</span>
            <span className="mma-kpi-value">{candidates.length} 건</span>
          </div>
          <div className="mma-kpi-card">
            <span className="mma-kpi-title">최근 15일 미식별 가맹점</span>
            <span className="mma-kpi-value warn">
              {unclassified.length} 건
            </span>
          </div>
        </section>

        {error && <div className="mma-alert">{error}</div>}

        <section className="mma-panel">
          <div className="mma-panel-header">
            <div>
              <div className="mma-panel-title">
                ⭐ 전역 마스터 승격 대기목록
              </div>
              <div className="mma-panel-sub">
                다수의 회원이 개별 학습한 가맹점 표기(per-user terms) 중 검증된
                항목을 전역 마스터(member_id IS NULL)로 승격합니다.
              </div>
            </div>
          </div>
          {isLoading ? (
            <div className="mma-empty">불러오는 중...</div>
          ) : candidates.length === 0 ? (
            <div className="mma-empty">승격 대기 중인 표기가 없습니다.</div>
          ) : (
            <table className="mma-table">
              <thead>
                <tr>
                  <th>원본 가맹점 표기</th>
                  <th>연결될 서비스</th>
                  <th>학습 회원 수</th>
                  <th>카테고리</th>
                  <th>승인</th>
                </tr>
              </thead>
              <tbody>
                {candidates.map((candidate) => {
                  const key = `${candidate.aliasId}:${candidate.aliasText}`;
                  return (
                    <tr key={key}>
                      <td>
                        <code>{candidate.aliasText}</code>
                      </td>
                      <td>
                        <span className="mma-badge purple">
                          {candidate.canonicalServiceName}
                        </span>
                      </td>
                      <td>
                        <strong>{candidate.learnerCount} 명</strong> 학습
                      </td>
                      <td>{candidate.categoryName || "미지정"}</td>
                      <td>
                        <button
                          type="button"
                          className="mma-btn mma-btn-primary mma-btn-sm"
                          disabled={promotingKey === key}
                          onClick={() => handlePromote(candidate)}
                        >
                          {promotingKey === key
                            ? "승격 중..."
                            : "승격 (Approve)"}
                        </button>
                      </td>
                    </tr>
                  );
                })}
              </tbody>
            </table>
          )}
        </section>

        <section className="mma-panel">
          <div className="mma-panel-header">
            <div>
              <div className="mma-panel-title">
                🔍 미식별 & &apos;기타&apos; 가맹점 작업대
              </div>
              <div className="mma-panel-sub">
                최근 15일간 전역 alias가 없는 상위 원본 가맹점 리스트입니다.
                바로 마스터에 등록하세요.
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
