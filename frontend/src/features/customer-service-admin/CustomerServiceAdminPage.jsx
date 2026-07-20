import { useEffect, useState } from "react";
import AdminLayout from "../../components/layout/AdminLayout";
import AdminInquiryDetailPanel from "./components/AdminInquiryDetailPanel";
import {
  getAdminInquiries,
  getInquiryCategories,
} from "../../api/customerServiceApi";
import { getApiErrorMessage } from "../../utils/apiError";
import "./CustomerServiceAdminPage.css";

const STATUS_LABEL = { PENDING: "답변 대기중", ANSWERED: "답변 완료" };

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

function CustomerServiceAdminPage() {
  const [items, setItems] = useState([]);
  const [totalElements, setTotalElements] = useState(0);
  const [categories, setCategories] = useState([]);
  const [status, setStatus] = useState("");
  const [categoryId, setCategoryId] = useState("");
  const [keyword, setKeyword] = useState("");
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState("");
  const [selectedInquiryId, setSelectedInquiryId] = useState(null);
  const [summary, setSummary] = useState({ total: 0, pending: 0 });

  useEffect(() => {
    getInquiryCategories()
      .then((data) => setCategories(data || []))
      .catch(() => {});
  }, []);

  const loadSummary = () =>
    Promise.all([
      getAdminInquiries({ size: 1 }),
      getAdminInquiries({ status: "PENDING", size: 1 }),
    ])
      .then(([all, pendingOnly]) => {
        setSummary({
          total: all?.totalElements || 0,
          pending: pendingOnly?.totalElements || 0,
        });
      })
      .catch(() => {
        // KPI 집계 실패는 조용히 무시하고 목록만 노출한다
      });

  useEffect(() => {
    loadSummary();
  }, []);

  const load = async () => {
    setIsLoading(true);
    setError("");
    try {
      const data = await getAdminInquiries({
        status: status || undefined,
        inquiryCategoryId: categoryId || undefined,
        keyword: keyword || undefined,
        size: 50,
      });
      setItems(data?.items || []);
      setTotalElements(data?.totalElements || 0);
    } catch (requestError) {
      setError(
        getApiErrorMessage(requestError, "문의 목록을 불러오지 못했어요."),
      );
    } finally {
      setIsLoading(false);
    }
  };

  useEffect(() => {
    const timer = window.setTimeout(load, 250);
    return () => window.clearTimeout(timer);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [status, categoryId, keyword]);

  const refreshAfterAnswer = () => {
    load();
    loadSummary();
  };

  return (
    <AdminLayout
      title="1:1 문의 / 고객센터 관리"
      subtitle="회원이 접수한 문의를 확인하고 답변을 등록합니다"
    >
      <div className="csa-page">
        <section className="csa-kpi-grid">
          <div className="csa-kpi-card">
            <span className="csa-kpi-title">전체 문의</span>
            <span className="csa-kpi-value">{summary.total} 건</span>
          </div>
          <div className="csa-kpi-card">
            <span className="csa-kpi-title">답변 대기중</span>
            <span className="csa-kpi-value warn">{summary.pending} 건</span>
          </div>
          <div className="csa-kpi-card">
            <span className="csa-kpi-title">답변 완료</span>
            <span className="csa-kpi-value">
              {Math.max(summary.total - summary.pending, 0)} 건
            </span>
          </div>
        </section>

        <section className="csa-filters">
          <select
            className="csa-select"
            value={status}
            onChange={(e) => setStatus(e.target.value)}
          >
            <option value="">전체 상태</option>
            <option value="PENDING">답변 대기중</option>
            <option value="ANSWERED">답변 완료</option>
          </select>
          <select
            className="csa-select"
            value={categoryId}
            onChange={(e) => setCategoryId(e.target.value)}
          >
            <option value="">전체 유형</option>
            {categories.map((category) => (
              <option key={category.id} value={category.id}>
                {category.name}
              </option>
            ))}
          </select>
          <input
            type="text"
            className="csa-input"
            placeholder="제목 또는 내용 검색"
            value={keyword}
            onChange={(e) => setKeyword(e.target.value)}
          />
        </section>

        <section className="csa-panel">
          <div className="csa-panel-header">
            <div>
              <div className="csa-panel-title">접수된 1:1 문의 목록</div>
              <div className="csa-panel-sub">
                사용자가 앱 및 고객센터 페이지에서 접수한 문의 내역입니다.
                클릭하여 답변을 등록하세요.
              </div>
            </div>
          </div>
          {error && <div className="csa-alert">{error}</div>}
          {isLoading ? (
            <div className="csa-empty">불러오는 중...</div>
          ) : items.length === 0 ? (
            <div className="csa-empty">조건에 맞는 문의가 없습니다.</div>
          ) : (
            <table className="csa-table">
              <thead>
                <tr>
                  <th>유형</th>
                  <th>제목</th>
                  <th>작성자</th>
                  <th>접수 일시</th>
                  <th>상태</th>
                  <th>관리</th>
                </tr>
              </thead>
              <tbody>
                {items.map((item) => (
                  <tr key={item.id}>
                    <td>
                      <span className="csa-badge blue">
                        {item.categoryName}
                      </span>
                    </td>
                    <td className="csa-table-title">{item.title}</td>
                    <td>{item.memberEmail}</td>
                    <td>{formatDateTime(item.createdAt)}</td>
                    <td>
                      <span
                        className={`csa-badge ${item.status === "ANSWERED" ? "green" : "yellow"}`}
                      >
                        {STATUS_LABEL[item.status] || item.status}
                      </span>
                    </td>
                    <td>
                      <button
                        type="button"
                        className={`csa-btn ${item.status === "ANSWERED" ? "csa-btn-secondary" : "csa-btn-primary"} csa-btn-sm`}
                        onClick={() => setSelectedInquiryId(item.id)}
                      >
                        {item.status === "ANSWERED" ? "답변 수정" : "답변하기"}
                      </button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          )}
          <div className="csa-total-count">총 {totalElements}건</div>
        </section>

        {selectedInquiryId && (
          <AdminInquiryDetailPanel
            inquiryId={selectedInquiryId}
            onClose={() => setSelectedInquiryId(null)}
            onAnswered={refreshAfterAnswer}
          />
        )}
      </div>
    </AdminLayout>
  );
}

export default CustomerServiceAdminPage;
