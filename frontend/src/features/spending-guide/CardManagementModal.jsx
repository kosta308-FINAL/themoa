import { useCallback, useEffect, useMemo, useState } from "react";
import {
  createCardConnection,
  getCardConnections,
  getCardIssuers,
  getInitialSyncStatus,
  retryInitialSync,
  updateCardSyncEnabled,
} from "../../api/spendingGuideApi";
import DashboardIcon from "../../components/common/DashboardIcon";

function CardManagementModal({ onClose, onChanged }) {
  const [connections, setConnections] = useState(null);
  const [issuers, setIssuers] = useState([]);
  const [syncStatus, setSyncStatus] = useState(null);
  const [form, setForm] = useState({
    organization: "",
    loginId: "",
    loginPassword: "",
    cardNo: "",
    cardPassword: "",
    birthDate: "",
  });
  const [error, setError] = useState("");
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [retryingId, setRetryingId] = useState(null);

  const selectedIssuer = useMemo(
    () => issuers.find((issuer) => issuer.organization === form.organization),
    [form.organization, issuers],
  );
  const update = (key) => (event) =>
    setForm((current) => ({ ...current, [key]: event.target.value }));

  const load = useCallback(async () => {
    try {
      const [connectionData, issuerData, statusData] = await Promise.all([
        getCardConnections(),
        getCardIssuers(),
        getInitialSyncStatus(),
      ]);
      setConnections(connectionData);
      setIssuers(issuerData || []);
      setSyncStatus(statusData);
    } catch (requestError) {
      setError(
        requestError.response?.data?.message ||
          "카드 연결 정보를 불러오지 못했습니다.",
      );
    }
  }, []);

  useEffect(() => {
    const timer = window.setTimeout(load, 0);
    return () => window.clearTimeout(timer);
  }, [load]);

  useEffect(() => {
    if (!["FETCHING", "ANALYZING"].includes(syncStatus?.overallStatus))
      return undefined;
    const timer = window.setTimeout(async () => {
      try {
        setSyncStatus(await getInitialSyncStatus());
      } catch {
        /* 다음 화면 진입 때 다시 조회 */
      }
    }, 3000);
    return () => window.clearTimeout(timer);
  }, [syncStatus]);

  const handleToggle = async () => {
    const enabled = !connections.cardSyncEnabled;
    setError("");
    try {
      await updateCardSyncEnabled(enabled);
      await load();
      await onChanged();
    } catch (requestError) {
      setError(
        requestError.response?.data?.message ||
          "자동수집 설정을 변경하지 못했습니다.",
      );
    }
  };

  const handleConnect = async (event) => {
    event.preventDefault();
    setError("");
    setIsSubmitting(true);
    try {
      await createCardConnection({
        organization: form.organization,
        loginId: form.loginId,
        loginPassword: form.loginPassword,
        cardNo: form.cardNo || null,
        cardPassword: form.cardPassword || null,
        birthDate: form.birthDate || null,
      });
      setForm({
        organization: "",
        loginId: "",
        loginPassword: "",
        cardNo: "",
        cardPassword: "",
        birthDate: "",
      });
      await load();
      await onChanged();
    } catch (requestError) {
      setError(
        requestError.response?.data?.message ||
          "카드를 연결하지 못했습니다. 입력 정보를 확인해주세요.",
      );
    } finally {
      setIsSubmitting(false);
    }
  };

  const handleRetry = async (connectionId) => {
    setRetryingId(connectionId);
    setError("");
    try {
      await retryInitialSync(connectionId);
      await load();
    } catch (requestError) {
      setError(
        requestError.response?.data?.message ||
          "초기 소비내역 수집을 다시 시작하지 못했습니다.",
      );
    } finally {
      setRetryingId(null);
    }
  };

  return (
    <div
      className="spending-modal-backdrop"
      role="presentation"
      onMouseDown={onClose}
    >
      <section
        className="spending-modal spending-card-modal"
        role="dialog"
        aria-modal="true"
        aria-labelledby="card-management-title"
        onMouseDown={(event) => event.stopPropagation()}
      >
        <div className="spending-modal-head">
          <div>
            <h2 id="card-management-title">카드 관리</h2>
            <p>카드사를 연결하고 소비내역 자동수집을 관리해요.</p>
          </div>
          <button
            type="button"
            className="spending-modal-close"
            onClick={onClose}
            aria-label="닫기"
          >
            ×
          </button>
        </div>
        <div className="spending-card-modal-body">
          {!connections ? (
            <div className="spending-loading">
              <span className="spending-spinner" />
              카드 정보를 불러오는 중...
            </div>
          ) : (
            <>
              <div className="spending-sync-setting">
                <div>
                  <strong>카드 자동수집</strong>
                  <p>연결과 기존 거래는 설정을 꺼도 유지됩니다.</p>
                </div>
                <button
                  type="button"
                  className={
                    connections.cardSyncEnabled
                      ? "spending-switch on"
                      : "spending-switch"
                  }
                  onClick={handleToggle}
                  aria-label={
                    connections.cardSyncEnabled
                      ? "자동수집 끄기"
                      : "자동수집 켜기"
                  }
                >
                  <i />
                </button>
              </div>
              {syncStatus?.overallStatus &&
                syncStatus.overallStatus !== "COMPLETED" && (
                  <div className="spending-form-notice">
                    <DashboardIcon name="info" size={17} />
                    <span>
                      초기 소비내역 수집 상태: {syncStatus.overallStatus}
                    </span>
                  </div>
                )}
              <div className="spending-connection-list">
                {connections.connections?.length ? (
                  connections.connections.map((connection) => (
                    <div key={connection.connectionId}>
                      <span className="spending-candidate-icon">
                        <DashboardIcon name="card" size={17} />
                      </span>
                      <div>
                        <strong>{connection.organizationName}</strong>
                        <p>
                          {connection.connectionStatus} · 초기수집{" "}
                          {connection.initialSyncStatus}
                        </p>
                      </div>
                      {connection.initialSyncStatus === "FAILED" && (
                        <button
                          type="button"
                          className="spending-retry-button"
                          disabled={retryingId === connection.connectionId}
                          onClick={() => handleRetry(connection.connectionId)}
                        >
                          {retryingId === connection.connectionId
                            ? "재시도 중..."
                            : "다시 수집"}
                        </button>
                      )}
                    </div>
                  ))
                ) : (
                  <EmptyConnection />
                )}
              </div>
            </>
          )}
          <form className="spending-card-connect-form" onSubmit={handleConnect}>
            <h3>카드사 추가 연결</h3>
            <label>
              <span>카드사 *</span>
              <select
                value={form.organization}
                onChange={update("organization")}
                required
              >
                <option value="" disabled>
                  카드사 선택
                </option>
                {issuers.map((issuer) => (
                  <option key={issuer.organization} value={issuer.organization}>
                    {issuer.name}
                  </option>
                ))}
              </select>
            </label>
            <label>
              <span>카드사 로그인 아이디 *</span>
              <input
                value={form.loginId}
                onChange={update("loginId")}
                autoComplete="username"
                required
              />
            </label>
            <label>
              <span>카드사 로그인 비밀번호 *</span>
              <input
                type="password"
                value={form.loginPassword}
                onChange={update("loginPassword")}
                autoComplete="current-password"
                required
              />
            </label>
            {selectedIssuer?.requiresCardCredentials && (
              <>
                <label>
                  <span>카드번호 *</span>
                  <input
                    value={form.cardNo}
                    onChange={update("cardNo")}
                    inputMode="numeric"
                    required
                  />
                </label>
                <label>
                  <span>카드 비밀번호 앞 2자리 *</span>
                  <input
                    type="password"
                    value={form.cardPassword}
                    onChange={update("cardPassword")}
                    inputMode="numeric"
                    maxLength={2}
                    required
                  />
                </label>
              </>
            )}
            {error && (
              <div className="spending-form-error">
                <DashboardIcon name="info" size={16} />
                {error}
              </div>
            )}
            <button
              type="submit"
              className="spending-primary"
              disabled={isSubmitting || !issuers.length}
            >
              {isSubmitting ? "연결 중..." : "카드 연결하기"}
            </button>
          </form>
        </div>
      </section>
    </div>
  );
}

function EmptyConnection() {
  return (
    <div className="spending-card-empty">
      <DashboardIcon name="card" size={20} />
      <span>아직 연결된 카드사가 없어요.</span>
    </div>
  );
}

export default CardManagementModal;
