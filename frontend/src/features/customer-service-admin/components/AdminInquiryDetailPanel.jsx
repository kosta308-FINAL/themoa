import { useCallback, useEffect, useState } from "react";
import MarkdownContent from "../../../components/common/MarkdownContent";
import {
  downloadAdminInquiryAttachment,
  getAdminInquiryDetail,
  upsertAdminInquiryAnswer,
} from "../../../api/customerServiceApi";
import { getApiErrorMessage } from "../../../utils/apiError";

const MACROS = [
  {
    label: "해외 결제 환율 차이 안내",
    text: "안녕하세요, 회원님! 더모아 고객센터입니다.\n\n해외 결제는 결제 시점의 카드사 환율과 해외이용수수료(약 1~1.5%)가 더해져 최종 원화 청구액에 약간의 차이가 발생할 수 있습니다.\n\n고정지출 상세 화면에서 실제 청구된 원화 금액으로 직접 보정하실 수 있습니다. 감사합니다!",
  },
  {
    label: "카드사 비밀번호 재시도 제한 안내",
    text: "안녕하세요, 회원님! 더모아 지원팀입니다.\n\n카드사 비밀번호를 3회 연속 잘못 입력하시면 회원님의 계정 보호를 위해 일정 시간 동안 카드 연동 재시도가 자동 제한됩니다.\n제한 시간이 지난 후 정확한 비밀번호로 다시 시도해 주시면 정상 연결됩니다.",
  },
  {
    label: "월급일 변경 반영 시점 안내",
    text: "안녕하세요, 회원님!\n\n월급일을 변경하시면 진행 중인 급여주기는 그대로 유지되고, 변경한 급여일은 다음 급여주기부터 반영됩니다. 이미 시작된 주기의 예산은 소급 재계산되지 않으니 안심하셔도 됩니다.",
  },
];

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

function isImageAttachment(attachment) {
  return Boolean(attachment.contentType?.startsWith("image/"));
}

function AdminInquiryDetailPanel({ inquiryId, onClose, onAnswered }) {
  const [detail, setDetail] = useState(null);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState("");
  const [answerText, setAnswerText] = useState("");
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [imagePreview, setImagePreview] = useState(null);
  const [isPreviewLoading, setIsPreviewLoading] = useState(false);

  const load = useCallback(async () => {
    setIsLoading(true);
    setError("");
    try {
      const data = await getAdminInquiryDetail(inquiryId);
      setDetail(data);
      setAnswerText(data?.answer?.contentMarkdown || "");
    } catch (requestError) {
      setError(
        getApiErrorMessage(requestError, "문의 상세를 불러오지 못했어요."),
      );
    } finally {
      setIsLoading(false);
    }
  }, [inquiryId]);

  useEffect(() => {
    const run = () => load();
    run();
  }, [load]);

  const handleDownload = async (attachment) => {
    try {
      const blob = await downloadAdminInquiryAttachment(
        inquiryId,
        attachment.id,
      );
      const url = window.URL.createObjectURL(blob);
      const link = document.createElement("a");
      link.href = url;
      link.download = attachment.originalFilename;
      link.click();
      window.URL.revokeObjectURL(url);
    } catch {
      setError("첨부파일을 불러오지 못했어요.");
    }
  };

  const handleAttachmentClick = async (attachment) => {
    if (!isImageAttachment(attachment)) {
      handleDownload(attachment);
      return;
    }
    setIsPreviewLoading(true);
    try {
      const blob = await downloadAdminInquiryAttachment(
        inquiryId,
        attachment.id,
      );
      const url = window.URL.createObjectURL(blob);
      setImagePreview({ url, filename: attachment.originalFilename });
    } catch {
      setError("첨부파일을 불러오지 못했어요.");
    } finally {
      setIsPreviewLoading(false);
    }
  };

  const closeImagePreview = () => {
    if (imagePreview) {
      window.URL.revokeObjectURL(imagePreview.url);
    }
    setImagePreview(null);
  };

  const handleSubmitAnswer = async () => {
    if (!answerText.trim()) {
      setError("답변 내용을 입력해 주세요.");
      return;
    }
    setIsSubmitting(true);
    setError("");
    try {
      const version = detail?.answer?.version ?? null;
      const updated = await upsertAdminInquiryAnswer(
        inquiryId,
        answerText.trim(),
        version,
      );
      setDetail((prev) => ({ ...prev, status: "ANSWERED", answer: updated }));
      onAnswered?.();
    } catch (requestError) {
      setError(
        getApiErrorMessage(
          requestError,
          "답변 등록에 실패했어요. 화면이 최신 상태가 아닐 수 있으니 새로고침 후 다시 시도해 주세요.",
        ),
      );
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <div className="csa-drawer-overlay" onClick={onClose}>
      <div className="csa-drawer" onClick={(e) => e.stopPropagation()}>
        <div className="csa-drawer-header">
          <h3>문의 상세 · 답변 작성</h3>
          <button type="button" className="csa-icon-btn" onClick={onClose}>
            ✕
          </button>
        </div>

        <div className="csa-drawer-body">
          {isLoading && <div className="csa-empty">불러오는 중...</div>}
          {error && <div className="csa-alert">{error}</div>}

          {detail && !isLoading && (
            <>
              <div className="csa-detail-meta">
                <span
                  className={`csa-badge ${detail.status === "ANSWERED" ? "green" : "yellow"}`}
                >
                  {STATUS_LABEL[detail.status] || detail.status}
                </span>
                <span className="csa-badge purple">{detail.categoryName}</span>
                <span className="csa-muted">{detail.memberEmail}</span>
                <span className="csa-muted">
                  {formatDateTime(detail.createdAt)}
                </span>
              </div>
              <h4 className="csa-detail-title">{detail.title}</h4>
              <p className="csa-detail-content">{detail.content}</p>

              {detail.attachments?.length > 0 && (
                <div className="csa-attachments">
                  {detail.attachments.map((attachment) => (
                    <button
                      key={attachment.id}
                      type="button"
                      className="csa-attachment-chip"
                      disabled={isPreviewLoading}
                      onClick={() => handleAttachmentClick(attachment)}
                    >
                      {isImageAttachment(attachment) ? "🖼" : "📎"}{" "}
                      {attachment.originalFilename}
                    </button>
                  ))}
                </div>
              )}

              {detail.answer && (
                <div className="csa-existing-answer">
                  <strong>현재 등록된 답변 미리보기</strong>
                  <MarkdownContent
                    markdown={detail.answer.contentMarkdown}
                    className="csa-markdown-preview"
                  />
                </div>
              )}

              <div className="csa-answer-form">
                <label className="csa-label">
                  자주 쓰는 매크로 답변 템플릿
                </label>
                <select
                  className="csa-select"
                  value=""
                  onChange={(e) => {
                    const macro = MACROS.find(
                      (m) => m.label === e.target.value,
                    );
                    if (macro) setAnswerText(macro.text);
                  }}
                >
                  <option value="">매크로 템플릿을 선택하세요</option>
                  {MACROS.map((macro) => (
                    <option key={macro.label} value={macro.label}>
                      {macro.label}
                    </option>
                  ))}
                </select>

                <label className="csa-label" style={{ marginTop: 14 }}>
                  답변 내용(Markdown) <span className="csa-required">*</span>
                </label>
                <textarea
                  className="csa-textarea"
                  value={answerText}
                  onChange={(e) => setAnswerText(e.target.value)}
                  placeholder="고객에게 전달할 답변을 입력하세요. 등록 시 작성자에게 앱 내 알림이 즉시 발송됩니다."
                  maxLength={20000}
                />

                <div className="csa-drawer-footer">
                  <button
                    type="button"
                    className="csa-btn csa-btn-secondary"
                    onClick={onClose}
                  >
                    취소
                  </button>
                  <button
                    type="button"
                    className="csa-btn csa-btn-primary"
                    onClick={handleSubmitAnswer}
                    disabled={isSubmitting}
                  >
                    {isSubmitting
                      ? "저장 중..."
                      : detail.answer
                        ? "답변 수정 저장"
                        : "답변 등록 및 알림 발송"}
                  </button>
                </div>
              </div>
            </>
          )}
        </div>
      </div>

      {imagePreview && (
        <div
          className="csa-image-preview-overlay"
          onClick={(e) => {
            e.stopPropagation();
            closeImagePreview();
          }}
        >
          <div
            className="csa-image-preview"
            onClick={(e) => e.stopPropagation()}
          >
            <div className="csa-image-preview-header">
              <span>{imagePreview.filename}</span>
              <button
                type="button"
                className="csa-icon-btn"
                onClick={closeImagePreview}
              >
                ✕
              </button>
            </div>
            <img src={imagePreview.url} alt={imagePreview.filename} />
            <div className="csa-image-preview-footer">
              <a
                className="csa-btn csa-btn-secondary"
                href={imagePreview.url}
                download={imagePreview.filename}
              >
                다운로드
              </a>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}

export default AdminInquiryDetailPanel;
