import { useCallback, useEffect, useState } from "react";
import { getAdminErrorLogDetail } from "../../../api/errorLogApi";
import { getApiErrorMessage } from "../../../utils/apiError";

function formatDateTime(value) {
  if (!value) return "-";
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return value;
  return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, "0")}-${String(
    date.getDate(),
  ).padStart(2, "0")} ${String(date.getHours()).padStart(2, "0")}:${String(
    date.getMinutes(),
  ).padStart(2, "0")}`;
}

function ErrorLogDetailDrawer({ errorLogId, onClose }) {
  const [detail, setDetail] = useState(null);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState("");

  const load = useCallback(async () => {
    setIsLoading(true);
    setError("");
    try {
      const data = await getAdminErrorLogDetail(errorLogId);
      setDetail(data);
    } catch (requestError) {
      setError(
        getApiErrorMessage(requestError, "오류 상세를 불러오지 못했어요."),
      );
    } finally {
      setIsLoading(false);
    }
  }, [errorLogId]);

  useEffect(() => {
    const run = () => load();
    run();
  }, [load]);

  const errorLog = detail?.errorLog;

  return (
    <div className="ela-drawer-overlay" onClick={onClose}>
      <div className="ela-drawer" onClick={(e) => e.stopPropagation()}>
        <div className="ela-drawer-header">
          <h3>오류 상세</h3>
          <button type="button" className="ela-icon-btn" onClick={onClose}>
            ✕
          </button>
        </div>

        <div className="ela-drawer-body">
          {isLoading && <div className="ela-empty">불러오는 중...</div>}
          {error && <div className="ela-alert">{error}</div>}

          {errorLog && !isLoading && (
            <>
              <div className="ela-detail-meta">
                <span className="ela-badge red">{errorLog.statusCode}</span>
                <span className="ela-muted">{errorLog.httpMethod}</span>
                <span className="ela-muted">{errorLog.requestUri}</span>
                <span className="ela-muted">
                  {formatDateTime(errorLog.createdAt)}
                </span>
              </div>

              <dl className="ela-detail-grid">
                <dt>traceId</dt>
                <dd>{errorLog.traceId}</dd>
                <dt>Controller</dt>
                <dd>{errorLog.controller}</dd>
                <dt>회원 ID</dt>
                <dd>{errorLog.memberId ?? "-"}</dd>
                <dt>예외 클래스</dt>
                <dd>{errorLog.exceptionClass}</dd>
              </dl>

              <div className="ela-detail-label">에러 메시지</div>
              <p className="ela-detail-content">{errorLog.errorMessage}</p>

              <div className="ela-detail-label">스택트레이스 (요약)</div>
              <pre className="ela-stacktrace">{errorLog.stackTraceExcerpt}</pre>
            </>
          )}
        </div>
      </div>
    </div>
  );
}

export default ErrorLogDetailDrawer;
