import { useEffect, useState } from "react";
import AdminLayout from "../../components/layout/AdminLayout";
import ErrorLogDetailDrawer from "./components/ErrorLogDetailDrawer";
import LogFileViewerPanel from "./components/LogFileViewerPanel";
import ApiPerformancePanel from "./components/ApiPerformancePanel";
import { getAdminErrorLogs } from "../../api/errorLogApi";
import { getApiErrorMessage } from "../../utils/apiError";
import "./ErrorLogAdminPage.css";

const DIAGNOSIS_LABEL = {
  PENDING: "AI 분석 중",
  COMPLETED: "AI 분석 완료",
  FAILED: "AI 분석 실패",
};

function diagnosisBadgeClass(status) {
  if (status === "COMPLETED") return "green";
  if (status === "PENDING") return "yellow";
  if (status === "FAILED") return "red";
  return "gray";
}

function formatDateTime(value) {
  if (!value) return "";
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return value;
  return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, "0")}-${String(
    date.getDate(),
  ).padStart(2, "0")} ${String(date.getHours()).padStart(2, "0")}:${String(
    date.getMinutes(),
  ).padStart(2, "0")}`;
}

function ErrorLogAdminPage() {
  const [activeTab, setActiveTab] = useState("db");
  const [items, setItems] = useState([]);
  const [totalElements, setTotalElements] = useState(0);
  const [exceptionClass, setExceptionClass] = useState("");
  const [requestUri, setRequestUri] = useState("");
  const [controller, setController] = useState("");
  const [memberId, setMemberId] = useState("");
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState("");
  const [selectedErrorLogId, setSelectedErrorLogId] = useState(null);
  const [totalCount, setTotalCount] = useState(0);

  useEffect(() => {
    getAdminErrorLogs({ size: 1 })
      .then((data) => setTotalCount(data?.totalElements || 0))
      .catch(() => {
        // KPI 집계 실패는 조용히 무시하고 목록만 노출한다
      });
  }, []);

  const load = async () => {
    setIsLoading(true);
    setError("");
    try {
      const data = await getAdminErrorLogs({
        exceptionClass: exceptionClass || undefined,
        requestUri: requestUri || undefined,
        controller: controller || undefined,
        memberId: memberId || undefined,
        size: 50,
      });
      setItems(data?.content || []);
      setTotalElements(data?.totalElements || 0);
    } catch (requestError) {
      setError(
        getApiErrorMessage(requestError, "에러 로그 목록을 불러오지 못했어요."),
      );
    } finally {
      setIsLoading(false);
    }
  };

  useEffect(() => {
    const timer = window.setTimeout(load, 250);
    return () => window.clearTimeout(timer);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [exceptionClass, requestUri, controller, memberId]);

  return (
    <AdminLayout
      title="오류 로그 관리"
      subtitle="예상하지 못한 서버 오류(500) 추적·AI 진단, 서버 파일 로그(WARN/ERROR) 열람, API 응답시간 통계를 제공합니다"
    >
      <div className="ela-page">
        <div className="ela-tabs">
          <button
            type="button"
            className={`ela-tab ${activeTab === "db" ? "active" : ""}`}
            onClick={() => setActiveTab("db")}
          >
            DB 에러 로그
          </button>
          <button
            type="button"
            className={`ela-tab ${activeTab === "files" ? "active" : ""}`}
            onClick={() => setActiveTab("files")}
          >
            파일 로그 (WARN / ERROR)
          </button>
          <button
            type="button"
            className={`ela-tab ${activeTab === "api-performance" ? "active" : ""}`}
            onClick={() => setActiveTab("api-performance")}
          >
            API 성능
          </button>
        </div>

        {activeTab === "files" && <LogFileViewerPanel />}
        {activeTab === "api-performance" && <ApiPerformancePanel />}

        {activeTab === "db" && (
          <>
            <section className="ela-kpi-grid">
              <div className="ela-kpi-card">
                <span className="ela-kpi-title">전체 오류</span>
                <span className="ela-kpi-value">{totalCount} 건</span>
              </div>
            </section>

            <section className="ela-filters">
              <input
                type="text"
                className="ela-input"
                placeholder="예외 클래스 (정확히 일치)"
                value={exceptionClass}
                onChange={(e) => setExceptionClass(e.target.value)}
              />
              <input
                type="text"
                className="ela-input"
                placeholder="요청 URI (정확히 일치)"
                value={requestUri}
                onChange={(e) => setRequestUri(e.target.value)}
              />
              <input
                type="text"
                className="ela-input"
                placeholder="Controller (예: CardConnectionController.connect)"
                value={controller}
                onChange={(e) => setController(e.target.value)}
              />
              <input
                type="number"
                className="ela-input ela-input-narrow"
                placeholder="회원 ID"
                value={memberId}
                onChange={(e) => setMemberId(e.target.value)}
              />
            </section>

            <section className="ela-panel">
              <div className="ela-panel-header">
                <div>
                  <div className="ela-panel-title">발생한 오류 목록</div>
                  <div className="ela-panel-sub">
                    500으로 응답한 예상하지 못한 오류와 서버 내부 실패를 모두
                    포함합니다. 클릭하면 상세와 AI 진단을 볼 수 있어요.
                  </div>
                </div>
              </div>
              {error && <div className="ela-alert">{error}</div>}
              {isLoading ? (
                <div className="ela-empty">불러오는 중...</div>
              ) : items.length === 0 ? (
                <div className="ela-empty">조건에 맞는 오류가 없습니다.</div>
              ) : (
                <table className="ela-table">
                  <thead>
                    <tr>
                      <th>발생 시각</th>
                      <th>HTTP</th>
                      <th>요청 URI</th>
                      <th>Controller</th>
                      <th>상태코드</th>
                      <th>예외 클래스</th>
                      <th>회원ID</th>
                      <th>AI 진단</th>
                    </tr>
                  </thead>
                  <tbody>
                    {items.map((item) => (
                      <tr
                        key={item.id}
                        className="ela-row-clickable"
                        onClick={() => setSelectedErrorLogId(item.id)}
                      >
                        <td>{formatDateTime(item.createdAt)}</td>
                        <td>{item.httpMethod}</td>
                        <td className="ela-table-uri">{item.requestUri}</td>
                        <td>{item.controller}</td>
                        <td>{item.statusCode}</td>
                        <td className="ela-table-exception">
                          {item.exceptionClass}
                        </td>
                        <td>{item.memberId ?? "-"}</td>
                        <td>
                          <span
                            className={`ela-badge ${diagnosisBadgeClass(item.diagnosisStatus)}`}
                          >
                            {DIAGNOSIS_LABEL[item.diagnosisStatus] || "미요청"}
                          </span>
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              )}
              <div className="ela-total-count">총 {totalElements}건</div>
            </section>

            {selectedErrorLogId && (
              <ErrorLogDetailDrawer
                errorLogId={selectedErrorLogId}
                onClose={() => setSelectedErrorLogId(null)}
              />
            )}
          </>
        )}
      </div>
    </AdminLayout>
  );
}

export default ErrorLogAdminPage;
