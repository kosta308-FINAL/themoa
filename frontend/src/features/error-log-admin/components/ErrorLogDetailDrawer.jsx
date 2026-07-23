import { useCallback, useEffect, useState } from "react";
import {
  getAdminErrorLogDetail,
  requestAdminErrorLogAiAnalysis,
} from "../../../api/errorLogApi";
import { getApiErrorMessage } from "../../../utils/apiError";

const DIAGNOSIS_LABEL = {
  PENDING: "AI 분석 중",
  COMPLETED: "AI 분석 완료",
  FAILED: "AI 분석 실패",
};

const CAUSE_CATEGORY_LABEL = {
  DATABASE: "데이터베이스",
  EXTERNAL_API: "외부 API",
  AUTH: "인증/인가",
  NULL_POINTER: "NULL 참조",
  BUSINESS_LOGIC: "비즈니스 로직",
  CONFIGURATION: "설정",
  UNKNOWN: "원인 미상",
};

function diagnosisBadgeClass(status) {
  if (status === "COMPLETED") return "green";
  if (status === "PENDING") return "yellow";
  if (status === "FAILED") return "red";
  return "gray";
}

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
  const [isRequesting, setIsRequesting] = useState(false);

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

  const handleRequestAnalysis = async () => {
    setIsRequesting(true);
    setError("");
    try {
      await requestAdminErrorLogAiAnalysis(errorLogId);
      await load();
    } catch (requestError) {
      setError(getApiErrorMessage(requestError, "AI 분석 요청에 실패했어요."));
    } finally {
      setIsRequesting(false);
    }
  };

  const errorLog = detail?.errorLog;
  const aiDiagnosis = detail?.aiDiagnosis;

  return (
    <div className="ela-drawer-overlay" onClick={onClose}>
      <div className="ela-drawer" onClick={(e) => e.stopPropagation()}>
        <div className="ela-drawer-header">
          <h3>오류 상세 · AI 진단</h3>
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

              <div className="ela-ai-section">
                <div className="ela-ai-header">
                  <span className="ela-detail-label" style={{ margin: 0 }}>
                    AI 진단
                  </span>
                  {aiDiagnosis && (
                    <span
                      className={`ela-badge ${diagnosisBadgeClass(aiDiagnosis.status)}`}
                    >
                      {DIAGNOSIS_LABEL[aiDiagnosis.status] ||
                        aiDiagnosis.status}
                    </span>
                  )}
                </div>

                {!aiDiagnosis && (
                  <p className="ela-muted">
                    아직 AI 분석을 요청한 적이 없습니다.
                  </p>
                )}

                {aiDiagnosis?.status === "COMPLETED" && (
                  <div className="ela-ai-result">
                    <div className="ela-ai-row">
                      <span className="ela-ai-row-label">원인 분류</span>
                      <span>
                        {CAUSE_CATEGORY_LABEL[aiDiagnosis.causeCategory] ||
                          aiDiagnosis.causeCategory}
                      </span>
                    </div>
                    <div className="ela-ai-row">
                      <span className="ela-ai-row-label">요약</span>
                      <span>{aiDiagnosis.summary}</span>
                    </div>
                    <div className="ela-ai-row">
                      <span className="ela-ai-row-label">원인 분석</span>
                      <span>{aiDiagnosis.rootCause}</span>
                    </div>
                    <div className="ela-ai-row">
                      <span className="ela-ai-row-label">권장 조치</span>
                      <span>{aiDiagnosis.recommendedAction}</span>
                    </div>
                    <div className="ela-ai-meta">
                      {aiDiagnosis.modelName} ·{" "}
                      {formatDateTime(aiDiagnosis.completedAt)} · 요청자 회원ID{" "}
                      {aiDiagnosis.requestedByMemberId ?? "-"}
                    </div>
                  </div>
                )}

                {aiDiagnosis?.status === "FAILED" && (
                  <p className="ela-alert">
                    {aiDiagnosis.failureMessage || "AI 분석에 실패했습니다."}
                  </p>
                )}

                {aiDiagnosis?.status === "PENDING" && (
                  <p className="ela-muted">분석이 진행 중입니다...</p>
                )}

                <button
                  type="button"
                  className="ela-btn ela-btn-primary"
                  onClick={handleRequestAnalysis}
                  disabled={isRequesting || aiDiagnosis?.status === "PENDING"}
                >
                  {isRequesting
                    ? "요청 중..."
                    : aiDiagnosis
                      ? "다시 분석 요청"
                      : "AI 분석 요청"}
                </button>
              </div>
            </>
          )}
        </div>
      </div>
    </div>
  );
}

export default ErrorLogDetailDrawer;
