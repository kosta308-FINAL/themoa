import { useEffect, useState } from "react";
import {
  createInquiry,
  getInquiryCategories,
} from "../../../api/customerServiceApi";
import { getApiErrorMessage } from "../../../utils/apiError";

const MAX_FILES = 3;
const MAX_FILE_SIZE_BYTES = 10 * 1024 * 1024;
const ALLOWED_TYPES = ["image/png", "image/jpeg"];

function InquiryForm({ onSubmitted, onCancel }) {
  const [categories, setCategories] = useState([]);
  const [inquiryCategoryId, setInquiryCategoryId] = useState("");
  const [title, setTitle] = useState("");
  const [content, setContent] = useState("");
  const [selectedFiles, setSelectedFiles] = useState([]);
  const [agreeTerms, setAgreeTerms] = useState(false);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [error, setError] = useState("");

  useEffect(() => {
    getInquiryCategories()
      .then((data) => setCategories(data || []))
      .catch(() =>
        setError("문의 유형을 불러오지 못했어요. 새로고침해 주세요."),
      );
  }, []);

  const handleFileChange = (e) => {
    const files = Array.from(e.target.files || []);
    if (files.length === 0) {
      setSelectedFiles([]);
      return;
    }
    if (files.length > MAX_FILES) {
      setError(`이미지는 최대 ${MAX_FILES}개까지 첨부할 수 있어요.`);
      e.target.value = "";
      return;
    }
    const invalid = files.find(
      (file) =>
        !ALLOWED_TYPES.includes(file.type) || file.size > MAX_FILE_SIZE_BYTES,
    );
    if (invalid) {
      setError(
        "PNG 또는 JPEG 이미지만, 파일당 최대 10MB까지 첨부할 수 있어요.",
      );
      e.target.value = "";
      return;
    }
    setError("");
    setSelectedFiles(files);
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    if (!inquiryCategoryId || !title.trim() || !content.trim() || !agreeTerms) {
      setError("필수 항목을 모두 입력해 주세요.");
      return;
    }
    setIsSubmitting(true);
    setError("");
    try {
      await createInquiry(
        {
          inquiryCategoryId: Number(inquiryCategoryId),
          title: title.trim(),
          content: content.trim(),
          agreedPrivacy: true,
        },
        selectedFiles,
      );
      onSubmitted?.();
    } catch (requestError) {
      setError(
        getApiErrorMessage(
          requestError,
          "문의 등록에 실패했어요. 잠시 후 다시 시도해 주세요.",
        ),
      );
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <div className="panel">
      <h2 style={{ margin: "0 0 8px", fontSize: "18px" }}>1:1 문의 작성하기</h2>
      <p
        style={{ margin: "0 0 24px", color: "var(--text-2)", fontSize: "14px" }}
      >
        궁금한 점이나 서비스 오류를 남겨주시면 확인 후 앱 내 알림으로 답변해
        드립니다.
      </p>

      {error && <div className="cs-inline-error">{error}</div>}

      <form onSubmit={handleSubmit}>
        <div className="form-group">
          <label className="form-label">
            문의 유형 <span className="req">*</span>
          </label>
          <select
            className="form-control"
            value={inquiryCategoryId}
            onChange={(e) => setInquiryCategoryId(e.target.value)}
            required
          >
            <option value="">문의 유형을 선택해 주세요</option>
            {categories.map((category) => (
              <option key={category.id} value={category.id}>
                {category.name}
              </option>
            ))}
          </select>
        </div>

        <div className="form-group">
          <label className="form-label">
            문의 제목 <span className="req">*</span>
          </label>
          <input
            type="text"
            className="form-control"
            value={title}
            onChange={(e) => setTitle(e.target.value)}
            placeholder="제목을 입력해 주세요 (예: 카드 연동 중 오류 코드가 뜹니다)"
            maxLength={200}
            required
          />
        </div>

        <div className="form-group">
          <label className="form-label">
            문의 내용 <span className="req">*</span>
          </label>
          <textarea
            className="form-control"
            value={content}
            onChange={(e) => setContent(e.target.value)}
            placeholder="자세한 문의 내용이나 발생 상황을 작성해 주시면 더욱 정확하고 빠른 답변이 가능합니다."
            maxLength={10000}
            required
          />
        </div>

        <div className="form-group">
          <label className="form-label">화면 캡처 파일 첨부 (선택)</label>
          <label htmlFor="fileInput" className="file-dropzone">
            <div className="icon-box">
              <svg
                width="20"
                height="20"
                viewBox="0 0 24 24"
                fill="none"
                stroke="currentColor"
                strokeWidth="2"
                strokeLinecap="round"
                strokeLinejoin="round"
              >
                <path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4" />
                <polyline points="17 8 12 3 7 8" />
                <line x1="12" y1="3" x2="12" y2="15" />
              </svg>
            </div>
            <p>이곳을 클릭하거나 이미지 파일(PNG, JPG)을 끌어다 놓으세요</p>
            <span>
              최대 10MB 이하의 이미지 파일 {MAX_FILES}개까지 첨부 가능
            </span>
            <input
              type="file"
              id="fileInput"
              hidden
              multiple
              accept="image/png,image/jpeg"
              onChange={handleFileChange}
            />
          </label>
          {selectedFiles.length > 0 && (
            <div
              style={{
                marginTop: "10px",
                fontSize: "12px",
                color: "var(--green-dark)",
                fontWeight: "650",
              }}
            >
              {selectedFiles.map((file) => (
                <div key={file.name}>
                  📎 {file.name} ({(file.size / 1024).toFixed(1)} KB)
                </div>
              ))}
            </div>
          )}
        </div>

        <div className="form-group">
          <label className="checkbox-label">
            <input
              type="checkbox"
              checked={agreeTerms}
              onChange={(e) => setAgreeTerms(e.target.checked)}
              required
            />
            <span>
              문의 처리 및 답변 안내를 위한{" "}
              <strong>개인정보 수집 및 이용</strong>에 동의합니다.
            </span>
          </label>
        </div>

        <div className="form-actions">
          <button
            type="button"
            className="secondary-btn"
            onClick={onCancel}
            disabled={isSubmitting}
          >
            취소
          </button>
          <button type="submit" className="primary-btn" disabled={isSubmitting}>
            {isSubmitting ? "등록 중..." : "문의 등록하기"}
          </button>
        </div>
      </form>
    </div>
  );
}

export default InquiryForm;
