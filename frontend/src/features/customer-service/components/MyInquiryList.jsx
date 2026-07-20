import { useEffect, useState } from "react";
import MarkdownContent from "../../../components/common/MarkdownContent";
import {
  downloadMyInquiryAttachment,
  getMyInquiryDetail,
  getMyInquiries,
} from "../../../api/customerServiceApi";
import { getApiErrorMessage } from "../../../utils/apiError";

const STATUS_LABEL = {
  PENDING: "답변 대기중",
  ANSWERED: "답변 완료",
};

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

function MyInquiryList({ refreshKey, focusInquiryId, onNewInquiry }) {
  const [items, setItems] = useState([]);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState("");
  const [expandedId, setExpandedId] = useState(null);
  const [details, setDetails] = useState({});
  const [detailError, setDetailError] = useState("");

  const load = async () => {
    setIsLoading(true);
    setError("");
    try {
      const data = await getMyInquiries({ size: 50 });
      setItems(data?.items || []);
    } catch (requestError) {
      setError(
        getApiErrorMessage(requestError, "문의 목록을 불러오지 못했어요."),
      );
    } finally {
      setIsLoading(false);
    }
  };

  const openInquiry = async (inquiryId) => {
    setExpandedId((prev) => (prev === inquiryId ? null : inquiryId));
    if (details[inquiryId]) return;
    setDetailError("");
    try {
      const detail = await getMyInquiryDetail(inquiryId);
      setDetails((prev) => ({ ...prev, [inquiryId]: detail }));
    } catch (requestError) {
      setDetailError(
        getApiErrorMessage(requestError, "문의 상세를 불러오지 못했어요."),
      );
    }
  };

  useEffect(() => {
    const run = () => load();
    run();
  }, [refreshKey]);

  useEffect(() => {
    const run = () => {
      if (focusInquiryId) {
        openInquiry(Number(focusInquiryId));
      }
    };
    run();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [focusInquiryId]);

  const handleDownload = async (inquiryId, attachment) => {
    try {
      const blob = await downloadMyInquiryAttachment(inquiryId, attachment.id);
      const url = window.URL.createObjectURL(blob);
      const link = document.createElement("a");
      link.href = url;
      link.download = attachment.originalFilename;
      link.click();
      window.URL.revokeObjectURL(url);
    } catch {
      setDetailError("첨부파일을 불러오지 못했어요.");
    }
  };

  if (isLoading) {
    return <div className="faq-empty">불러오는 중...</div>;
  }

  return (
    <div>
      <div
        style={{
          display: "flex",
          justifyContent: "space-between",
          alignItems: "center",
          marginBottom: "18px",
        }}
      >
        <h2 style={{ margin: 0, fontSize: "18px" }}>내가 작성한 문의 목록</h2>
        <button
          type="button"
          className="secondary-btn"
          style={{ minHeight: "36px", fontSize: "13px" }}
          onClick={onNewInquiry}
        >
          + 새 문의 작성
        </button>
      </div>

      {error && <div className="cs-inline-error">{error}</div>}
      {detailError && <div className="cs-inline-error">{detailError}</div>}

      {items.length === 0 && !error && (
        <div className="faq-empty">아직 작성한 문의가 없어요.</div>
      )}

      <div>
        {items.map((item) => {
          const isOpen = expandedId === item.id;
          const detail = details[item.id];
          return (
            <div key={item.id} className="inquiry-card">
              <button
                type="button"
                className="inquiry-card-trigger"
                onClick={() => openInquiry(item.id)}
              >
                <div className="inquiry-header">
                  <span
                    className={`inquiry-status ${item.status === "ANSWERED" ? "done" : "pending"}`}
                  >
                    {STATUS_LABEL[item.status] || item.status}
                  </span>
                  <span className="inquiry-date">
                    {formatDateTime(item.createdAt)}
                  </span>
                </div>
                <h3 className="inquiry-title">
                  [{item.categoryName}] {item.title}
                </h3>
              </button>

              {isOpen && (
                <div style={{ padding: "0 4px 4px" }}>
                  {!detail && <div className="faq-empty">불러오는 중...</div>}
                  {detail && (
                    <>
                      <p
                        style={{
                          margin: "0 0 10px",
                          fontSize: "13px",
                          color: "var(--text-2)",
                          whiteSpace: "pre-wrap",
                        }}
                      >
                        {detail.content}
                      </p>
                      {detail.attachments?.length > 0 && (
                        <div className="inquiry-attachments">
                          {detail.attachments.map((attachment) => (
                            <button
                              key={attachment.id}
                              type="button"
                              className="attachment-chip"
                              onClick={() =>
                                handleDownload(item.id, attachment)
                              }
                            >
                              📎 {attachment.originalFilename}
                            </button>
                          ))}
                        </div>
                      )}
                      {detail.answer && (
                        <div className="inquiry-response">
                          <strong>
                            💬 더모아 고객센터 답변 (
                            {formatDateTime(
                              detail.answer.updatedAt ||
                                detail.answer.createdAt,
                            )}
                            )
                          </strong>
                          <MarkdownContent
                            markdown={detail.answer.contentMarkdown}
                            className="inquiry-response-body"
                          />
                        </div>
                      )}
                    </>
                  )}
                </div>
              )}
            </div>
          );
        })}
      </div>
    </div>
  );
}

export default MyInquiryList;
