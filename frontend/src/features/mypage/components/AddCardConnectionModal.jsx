import { useEffect, useMemo, useState } from "react";
import DashboardIcon from "../../../components/common/DashboardIcon";
import {
  createCardConnection,
  getCardIssuers,
} from "../../../api/spendingGuideApi";
import { getApiErrorMessage } from "../../../utils/apiError";

function AddCardConnectionModal({ onClose, onConnected }) {
  const [issuers, setIssuers] = useState([]);
  const [isLoadingIssuers, setIsLoadingIssuers] = useState(true);
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

  useEffect(() => {
    const run = async () => {
      try {
        setIssuers((await getCardIssuers()) || []);
      } catch (requestError) {
        setError(
          getApiErrorMessage(requestError, "카드사 목록을 불러오지 못했어요."),
        );
      } finally {
        setIsLoadingIssuers(false);
      }
    };
    run();
  }, []);

  const selectedIssuer = useMemo(
    () => issuers.find((issuer) => issuer.organization === form.organization),
    [form.organization, issuers],
  );
  const update = (key) => (event) =>
    setForm((current) => ({ ...current, [key]: event.target.value }));

  const handleSubmit = async (event) => {
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
      await onConnected();
    } catch (requestError) {
      setError(
        getApiErrorMessage(
          requestError,
          "카드를 연결하지 못했어요. 입력 정보를 확인해주세요.",
        ),
      );
      setIsSubmitting(false);
    }
  };

  return (
    <div
      className="mp-modal-backdrop"
      role="presentation"
      onMouseDown={onClose}
    >
      <section
        className="mp-modal"
        role="dialog"
        aria-modal="true"
        aria-labelledby="mp-add-card-title"
        onMouseDown={(event) => event.stopPropagation()}
      >
        <div className="mp-modal-head">
          <div>
            <h2 id="mp-add-card-title">카드사 추가 연결</h2>
            <p>카드사 로그인 정보로 결제내역 자동수집을 연결해요.</p>
          </div>
          <button
            type="button"
            className="mp-modal-close"
            onClick={onClose}
            aria-label="닫기"
          >
            ×
          </button>
        </div>
        <form className="mp-inline-form" onSubmit={handleSubmit}>
          <label>
            <span>카드사</span>
            <select
              value={form.organization}
              onChange={update("organization")}
              required
              disabled={isLoadingIssuers}
            >
              <option value="" disabled>
                {isLoadingIssuers ? "불러오는 중..." : "카드사 선택"}
              </option>
              {issuers.map((issuer) => (
                <option key={issuer.organization} value={issuer.organization}>
                  {issuer.name}
                </option>
              ))}
            </select>
          </label>
          <label>
            <span>카드사 로그인 아이디</span>
            <input
              value={form.loginId}
              onChange={update("loginId")}
              autoComplete="username"
              required
            />
          </label>
          <label>
            <span>카드사 로그인 비밀번호</span>
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
                <span>카드번호</span>
                <input
                  value={form.cardNo}
                  onChange={update("cardNo")}
                  inputMode="numeric"
                  required
                />
              </label>
              <label>
                <span>카드 비밀번호 앞 2자리</span>
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
            <div className="mp-form-error">
              <DashboardIcon name="info" size={16} />
              {error}
            </div>
          )}
          <button
            type="submit"
            className="mp-primary-button"
            disabled={isSubmitting || !issuers.length}
          >
            {isSubmitting ? "연결 중..." : "카드 연결하기"}
          </button>
        </form>
      </section>
    </div>
  );
}

export default AddCardConnectionModal;
