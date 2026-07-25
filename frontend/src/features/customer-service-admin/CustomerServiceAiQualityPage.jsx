import { useEffect, useMemo, useRef, useState } from "react";
import AdminLayout from "../../components/layout/AdminLayout";
import {
  createAdminCustomerKnowledgeText,
  disableAdminCustomerKnowledgeDocument,
  getAdminCustomerAiSettings,
  getAdminCustomerKnowledgeDocuments,
  getAdminCustomerKnowledgeMetadataOptions,
  previewAdminCustomerAiAnswer,
  previewAdminCustomerKnowledgeChunks,
  reembedAdminCustomerKnowledgeDocument,
  searchAdminCustomerAiKnowledge,
  updateAdminCustomerAiSettings,
  uploadAdminCustomerKnowledgeDocument,
} from "../../api/customerServiceApi";
import { getApiErrorMessage } from "../../utils/apiError";
import "./CustomerServiceAiQualityPage.css";

const STATUS_LABEL = {
  EMBEDDED: "임베딩 완료",
  FAILED: "임베딩 실패",
  DISABLED: "비활성화",
};

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

function formatScore(value) {
  if (value === null || value === undefined) return "-";
  return Number(value).toFixed(4);
}

function formatKnowledgeSourceName(value) {
  return value === "direct-text.txt" ? "직접 입력" : value;
}

function formatSplitMode(splitByMarkdownHeading, splitByParagraph) {
  const labels = [];
  if (splitByMarkdownHeading) labels.push("제목(#) 우선 분리");
  if (splitByParagraph) labels.push("문단 단위 분리");
  return labels.length > 0 ? labels.join(" + ") : "구간 자동 병합";
}

function CustomerServiceAiQualityPage() {
  const [query, setQuery] = useState(
    "처음 카드동기화 했는데 1분넘도록 동기화가 안됨",
  );
  const [topK, setTopK] = useState(6);
  const [minimumSimilarity, setMinimumSimilarity] = useState(0.45);
  const [systemPrompt, setSystemPrompt] = useState("");
  const [searchResult, setSearchResult] = useState(null);
  const [answerPreview, setAnswerPreview] = useState("");
  const [documents, setDocuments] = useState([]);
  const [metadataOptions, setMetadataOptions] = useState({
    titles: [],
    categories: [],
  });
  const [knowledgeInputMode, setKnowledgeInputMode] = useState("text");
  const [uploadTitle, setUploadTitle] = useState("");
  const [uploadCategory, setUploadCategory] = useState("고객센터 운영정책");
  const [uploadFile, setUploadFile] = useState(null);
  const [knowledgeText, setKnowledgeText] = useState("");
  const [chunkMaxLength, setChunkMaxLength] = useState(1200);
  const [chunkMinLength, setChunkMinLength] = useState(200);
  const [chunkOverlapLength, setChunkOverlapLength] = useState(150);
  const [splitByMarkdownHeading, setSplitByMarkdownHeading] = useState(false);
  const [splitByParagraph, setSplitByParagraph] = useState(false);
  const [chunkPreview, setChunkPreview] = useState(null);
  const [chunkPreviewLoading, setChunkPreviewLoading] = useState(false);
  const [chunkPreviewError, setChunkPreviewError] = useState("");
  const uploadFileTextRef = useRef("");
  const chunkPreviewTimerRef = useRef(null);
  const chunkPreviewRequestIdRef = useRef(0);
  const [notice, setNotice] = useState("");
  const [error, setError] = useState("");
  const [busyKey, setBusyKey] = useState("");

  const embeddedCount = useMemo(
    () => documents.filter((item) => item.status === "EMBEDDED").length,
    [documents],
  );

  const loadSettings = async () => {
    const settings = await getAdminCustomerAiSettings();
    setTopK(settings.topK);
    setMinimumSimilarity(settings.minimumSimilarity);
    setSystemPrompt(settings.systemPrompt || "");
  };

  const loadDocuments = async () => {
    const data = await getAdminCustomerKnowledgeDocuments();
    setDocuments(data || []);
  };

  const loadMetadataOptions = async () => {
    const data = await getAdminCustomerKnowledgeMetadataOptions();
    setMetadataOptions({
      titles: data?.titles || [],
      categories: data?.categories || [],
    });
  };

  useEffect(() => {
    const timer = setTimeout(() => {
      Promise.all([loadSettings(), loadDocuments(), loadMetadataOptions()]).catch(
        (requestError) => {
          setError(
            getApiErrorMessage(
              requestError,
              "AI 품질관리 정보를 불러오지 못했어요.",
            ),
          );
        },
      );
    }, 0);
    return () => clearTimeout(timer);
  }, []);

  useEffect(() => {
    return () => {
      if (chunkPreviewTimerRef.current) {
        clearTimeout(chunkPreviewTimerRef.current);
      }
    };
  }, []);

  useEffect(() => {
    if (!notice) return;
    const timer = setTimeout(() => setNotice(""), 4000);
    return () => clearTimeout(timer);
  }, [notice]);

  useEffect(() => {
    if (!error) return;
    const timer = setTimeout(() => setError(""), 6000);
    return () => clearTimeout(timer);
  }, [error]);

  const scheduleChunkPreview = (content, optionOverrides = {}) => {
    if (chunkPreviewTimerRef.current) {
      clearTimeout(chunkPreviewTimerRef.current);
    }
    const trimmed = (content || "").trim();
    const requestId = ++chunkPreviewRequestIdRef.current;
    if (!trimmed) {
      setChunkPreview(null);
      setChunkPreviewError("");
      setChunkPreviewLoading(false);
      return;
    }
    setChunkPreviewLoading(true);
    setChunkPreviewError("");
    const options = {
      chunkMaxLength: Number(optionOverrides.chunkMaxLength ?? chunkMaxLength),
      chunkMinLength: Number(optionOverrides.chunkMinLength ?? chunkMinLength),
      chunkOverlapLength: Number(
        optionOverrides.chunkOverlapLength ?? chunkOverlapLength,
      ),
      splitByMarkdownHeading:
        optionOverrides.splitByMarkdownHeading ?? splitByMarkdownHeading,
      splitByParagraph: optionOverrides.splitByParagraph ?? splitByParagraph,
    };
    chunkPreviewTimerRef.current = setTimeout(async () => {
      try {
        const data = await previewAdminCustomerKnowledgeChunks({
          content,
          ...options,
        });
        if (chunkPreviewRequestIdRef.current === requestId) {
          setChunkPreview(data);
        }
      } catch (requestError) {
        if (chunkPreviewRequestIdRef.current === requestId) {
          setChunkPreview(null);
          setChunkPreviewError(
            getApiErrorMessage(requestError, "청킹 미리보기에 실패했어요."),
          );
        }
      } finally {
        if (chunkPreviewRequestIdRef.current === requestId) {
          setChunkPreviewLoading(false);
        }
      }
    }, 400);
  };

  const currentKnowledgeContent = () =>
    knowledgeInputMode === "text" ? knowledgeText : uploadFileTextRef.current;

  const handleKnowledgeTextChange = (value) => {
    setKnowledgeText(value);
    if (knowledgeInputMode === "text") {
      scheduleChunkPreview(value);
    }
  };

  const handleKnowledgeInputModeChange = (mode) => {
    setKnowledgeInputMode(mode);
    scheduleChunkPreview(
      mode === "text" ? knowledgeText : uploadFileTextRef.current,
    );
  };

  const handleUploadFileChange = (file) => {
    setUploadFile(file);
    if (!file) {
      uploadFileTextRef.current = "";
      scheduleChunkPreview("");
      return;
    }
    file
      .text()
      .then((text) => {
        uploadFileTextRef.current = text;
        scheduleChunkPreview(text);
      })
      .catch(() => {
        uploadFileTextRef.current = "";
        scheduleChunkPreview("");
      });
  };

  const handleChunkMaxLengthChange = (value) => {
    setChunkMaxLength(value);
    scheduleChunkPreview(currentKnowledgeContent(), { chunkMaxLength: value });
  };

  const handleChunkMinLengthChange = (value) => {
    setChunkMinLength(value);
    scheduleChunkPreview(currentKnowledgeContent(), { chunkMinLength: value });
  };

  const handleChunkOverlapLengthChange = (value) => {
    setChunkOverlapLength(value);
    scheduleChunkPreview(currentKnowledgeContent(), {
      chunkOverlapLength: value,
    });
  };

  const handleSplitByMarkdownHeadingChange = (checked) => {
    setSplitByMarkdownHeading(checked);
    scheduleChunkPreview(currentKnowledgeContent(), {
      splitByMarkdownHeading: checked,
    });
  };

  const handleSplitByParagraphChange = (checked) => {
    setSplitByParagraph(checked);
    scheduleChunkPreview(currentKnowledgeContent(), {
      splitByParagraph: checked,
    });
  };

  const clearChunkPreview = () => {
    chunkPreviewRequestIdRef.current += 1;
    if (chunkPreviewTimerRef.current) {
      clearTimeout(chunkPreviewTimerRef.current);
    }
    setChunkPreview(null);
    setChunkPreviewError("");
    setChunkPreviewLoading(false);
  };

  const runSearch = async () => {
    setBusyKey("search");
    setError("");
    setNotice("");
    setAnswerPreview("");
    try {
      const data = await searchAdminCustomerAiKnowledge({
        query,
        topK: Number(topK),
        minimumSimilarity: Number(minimumSimilarity),
      });
      setSearchResult(data);
    } catch (requestError) {
      setError(
        getApiErrorMessage(requestError, "Qdrant 검색 테스트에 실패했어요."),
      );
    } finally {
      setBusyKey("");
    }
  };

  const runPreview = async () => {
    setBusyKey("preview");
    setError("");
    setNotice("");
    try {
      const data = await previewAdminCustomerAiAnswer({
        query,
        topK: Number(topK),
        minimumSimilarity: Number(minimumSimilarity),
        systemPrompt,
      });
      setSearchResult(data.search);
      setAnswerPreview(data.answerMarkdown || "");
    } catch (requestError) {
      setError(getApiErrorMessage(requestError, "답변 미리보기에 실패했어요."));
    } finally {
      setBusyKey("");
    }
  };

  const saveSettings = async () => {
    setBusyKey("settings");
    setError("");
    setNotice("");
    try {
      const data = await updateAdminCustomerAiSettings({
        topK: Number(topK),
        minimumSimilarity: Number(minimumSimilarity),
        systemPrompt,
      });
      setTopK(data.topK);
      setMinimumSimilarity(data.minimumSimilarity);
      setSystemPrompt(data.systemPrompt || "");
      setNotice("운영 기본값을 저장했어요.");
    } catch (requestError) {
      setError(
        getApiErrorMessage(requestError, "운영 기본값 저장에 실패했어요."),
      );
    } finally {
      setBusyKey("");
    }
  };

  const uploadDocument = async (event) => {
    event.preventDefault();
    if (!uploadTitle.trim()) {
      setError("문서 제목을 입력해 주세요.");
      return;
    }
    if (!uploadCategory.trim()) {
      setError("카테고리를 입력해 주세요.");
      return;
    }
    if (!uploadFile) {
      setError("업로드할 md 또는 txt 파일을 선택해 주세요.");
      return;
    }
    setBusyKey("upload");
    setError("");
    setNotice("");
    try {
      await uploadAdminCustomerKnowledgeDocument({
        title: uploadTitle,
        category: uploadCategory,
        file: uploadFile,
        chunkMaxLength: Number(chunkMaxLength),
        chunkMinLength: Number(chunkMinLength),
        chunkOverlapLength: Number(chunkOverlapLength),
        splitByMarkdownHeading,
        splitByParagraph,
      });
      setUploadTitle("");
      setUploadFile(null);
      uploadFileTextRef.current = "";
      clearChunkPreview();
      event.target.reset();
      await Promise.all([loadDocuments(), loadMetadataOptions()]);
      setNotice("문서를 청킹하고 Qdrant에 임베딩했어요.");
    } catch (requestError) {
      setError(
        getApiErrorMessage(
          requestError,
          "문서 업로드 또는 임베딩에 실패했어요.",
        ),
      );
    } finally {
      setBusyKey("");
    }
  };

  const createTextDocument = async (event) => {
    event.preventDefault();
    if (!uploadTitle.trim()) {
      setError("문서 제목을 입력해 주세요.");
      return;
    }
    if (!uploadCategory.trim()) {
      setError("카테고리를 입력해 주세요.");
      return;
    }
    if (!knowledgeText.trim()) {
      setError("등록할 내용을 입력해 주세요.");
      return;
    }
    setBusyKey("text");
    setError("");
    setNotice("");
    try {
      await createAdminCustomerKnowledgeText({
        title: uploadTitle,
        category: uploadCategory,
        content: knowledgeText,
        chunkMaxLength: Number(chunkMaxLength),
        chunkMinLength: Number(chunkMinLength),
        chunkOverlapLength: Number(chunkOverlapLength),
        splitByMarkdownHeading,
        splitByParagraph,
      });
      setUploadTitle("");
      setKnowledgeText("");
      clearChunkPreview();
      await Promise.all([loadDocuments(), loadMetadataOptions()]);
      setNotice("입력한 텍스트를 청킹하고 Qdrant에 임베딩했어요.");
    } catch (requestError) {
      setError(
        getApiErrorMessage(
          requestError,
          "텍스트 입력 또는 임베딩에 실패했어요.",
        ),
      );
    } finally {
      setBusyKey("");
    }
  };

  const reembedDocument = async (documentId) => {
    setBusyKey(`reembed-${documentId}`);
    setError("");
    setNotice("");
    try {
      await reembedAdminCustomerKnowledgeDocument(documentId);
      await loadDocuments();
      setNotice("문서를 다시 임베딩했어요.");
    } catch (requestError) {
      setError(getApiErrorMessage(requestError, "재임베딩에 실패했어요."));
    } finally {
      setBusyKey("");
    }
  };

  const disableDocument = async (documentId) => {
    setBusyKey(`disable-${documentId}`);
    setError("");
    setNotice("");
    try {
      await disableAdminCustomerKnowledgeDocument(documentId);
      await loadDocuments();
      setNotice(
        "문서를 비활성화했어요. 다음 검색부터 컨텍스트 후보에서 제외됩니다.",
      );
    } catch (requestError) {
      setError(getApiErrorMessage(requestError, "문서 비활성화에 실패했어요."));
    } finally {
      setBusyKey("");
    }
  };

  return (
    <AdminLayout
      title="FAQ AI 품질관리"
      subtitle="고객센터 RAG 검색 결과와 답변 품질을 점검합니다"
    >
      <div className="aiq-page">
        <section className="aiq-kpi-grid">
          <div className="aiq-kpi-card">
            <span>운영 topK</span>
            <strong>{topK}</strong>
          </div>
          <div className="aiq-kpi-card">
            <span>최소 유사도</span>
            <strong>{Number(minimumSimilarity).toFixed(2)}</strong>
          </div>
          <div className="aiq-kpi-card">
            <span>업로드 문서</span>
            <strong>
              {embeddedCount}/{documents.length}
            </strong>
          </div>
        </section>

        {(notice || error) && (
          <div className="aiq-toast-stack" role="status" aria-live="polite">
            {notice && <div className="aiq-toast success">{notice}</div>}
            {error && <div className="aiq-toast error">{error}</div>}
          </div>
        )}

        <section className="aiq-panel aiq-playground">
          <div className="aiq-panel-heading">
            <div>
              <h2>Qdrant 검색 / 답변 테스트</h2>
              <p>
                관리자가 입력한 질문으로 검색 점수와 최종 답변을 함께
                확인합니다.
              </p>
            </div>
            <button
              type="button"
              className="aiq-btn aiq-btn-secondary"
              onClick={saveSettings}
              disabled={Boolean(busyKey)}
            >
              운영 기본값 저장
            </button>
          </div>

          <div className="aiq-form-grid">
            <label className="aiq-field aiq-field-wide">
              <span>테스트 질문</span>
              <input
                value={query}
                onChange={(event) => setQuery(event.target.value)}
                placeholder="예: 첫 카드 동기화가 1분 넘게 안 끝나요"
              />
            </label>
            <label className="aiq-field">
              <span>topK</span>
              <input
                type="number"
                min="1"
                max="20"
                value={topK}
                onChange={(event) => setTopK(event.target.value)}
              />
            </label>
            <label className="aiq-field">
              <span>최소 유사도</span>
              <input
                type="number"
                min="0"
                max="1"
                step="0.01"
                value={minimumSimilarity}
                onChange={(event) => setMinimumSimilarity(event.target.value)}
              />
            </label>
            <label className="aiq-field aiq-field-wide">
              <span>시스템 프롬프트</span>
              <textarea
                value={systemPrompt}
                onChange={(event) => setSystemPrompt(event.target.value)}
                rows={8}
              />
            </label>
          </div>

          <div className="aiq-actions">
            <button
              type="button"
              className="aiq-btn aiq-btn-secondary"
              onClick={runSearch}
              disabled={Boolean(busyKey)}
            >
              Qdrant 검색만 실행
            </button>
            <button
              type="button"
              className="aiq-btn aiq-btn-primary"
              onClick={runPreview}
              disabled={Boolean(busyKey)}
            >
              답변까지 실행
            </button>
          </div>
        </section>

        <section className="aiq-results-grid">
          <div className="aiq-panel">
            <div className="aiq-panel-heading compact">
              <h2>검색 결과</h2>
              {searchResult && <span>{searchResult.resultCount}개 문서</span>}
            </div>
            {!searchResult ? (
              <div className="aiq-empty">아직 실행한 검색이 없습니다.</div>
            ) : searchResult.results.length === 0 ? (
              <div className="aiq-empty">Qdrant 검색 결과가 없습니다.</div>
            ) : (
              <div className="aiq-result-list">
                {searchResult.results.map((item) => (
                  <article
                    className="aiq-result-item"
                    key={`${item.sourceType}-${item.sourceId}-${item.rank}`}
                  >
                    <div className="aiq-result-meta">
                      <strong>#{item.rank}</strong>
                      <span>score {formatScore(item.score)}</span>
                      <span>{item.sourceType}</span>
                      <span>{item.category}</span>
                    </div>
                    <h3>{item.title}</h3>
                    <p>{item.content}</p>
                  </article>
                ))}
              </div>
            )}
          </div>

          <div className="aiq-panel">
            <div className="aiq-panel-heading compact">
              <h2>답변 미리보기</h2>
            </div>
            {answerPreview ? (
              <pre className="aiq-answer-preview">{answerPreview}</pre>
            ) : (
              <div className="aiq-empty">
                답변까지 실행하면 여기에 결과가 표시됩니다.
              </div>
            )}
          </div>
        </section>

        <section className="aiq-panel">
          <div className="aiq-panel-heading">
            <div>
              <h2>고객센터 지식 추가</h2>
              <p>
                파일을 올리거나 일반 텍스트를 입력하면 청킹 후 고객센터 Qdrant
                컬렉션에 임베딩됩니다.
              </p>
            </div>
          </div>

          <div className="aiq-segmented" aria-label="지식 추가 방식">
            <button
              type="button"
              className={knowledgeInputMode === "text" ? "active" : ""}
              onClick={() => handleKnowledgeInputModeChange("text")}
            >
              직접 입력
            </button>
            <button
              type="button"
              className={knowledgeInputMode === "file" ? "active" : ""}
              onClick={() => handleKnowledgeInputModeChange("file")}
            >
              md/txt 파일
            </button>
          </div>

          <datalist id="aiq-title-options">
            {metadataOptions.titles.map((title) => (
              <option value={title} key={title} />
            ))}
          </datalist>
          <datalist id="aiq-category-options">
            {metadataOptions.categories.map((category) => (
              <option value={category} key={category} />
            ))}
          </datalist>

          <form
            className={
              knowledgeInputMode === "text"
                ? "aiq-upload-form text-mode"
                : "aiq-upload-form"
            }
            onSubmit={
              knowledgeInputMode === "text"
                ? createTextDocument
                : uploadDocument
            }
          >
            <input
              type="text"
              placeholder="문서 제목 선택 또는 신규 입력"
              list="aiq-title-options"
              value={uploadTitle}
              onChange={(event) => setUploadTitle(event.target.value)}
            />
            <input
              type="text"
              placeholder="카테고리 선택 또는 신규 입력"
              list="aiq-category-options"
              value={uploadCategory}
              onChange={(event) => setUploadCategory(event.target.value)}
            />
            {knowledgeInputMode === "text" ? (
              <textarea
                className="aiq-knowledge-textarea"
                placeholder="고객센터에서 답변 근거로 쓸 내용을 자유롭게 입력하세요."
                value={knowledgeText}
                onChange={(event) =>
                  handleKnowledgeTextChange(event.target.value)
                }
                rows={8}
              />
            ) : (
              <label className="aiq-file-picker">
                <span className="aiq-file-picker-btn">파일 선택</span>
                <span className="aiq-file-picker-name">
                  {uploadFile
                    ? uploadFile.name
                    : "선택된 파일 없음 (.md, .txt)"}
                </span>
                <input
                  type="file"
                  accept=".md,.txt,text/markdown,text/plain"
                  onChange={(event) =>
                    handleUploadFileChange(event.target.files?.[0] || null)
                  }
                />
              </label>
            )}
            <button
              type="submit"
              className="aiq-btn aiq-btn-primary"
              disabled={Boolean(busyKey)}
            >
              {knowledgeInputMode === "text"
                ? "텍스트 임베딩"
                : "청킹 후 임베딩"}
            </button>
          </form>

          <div className="aiq-chunk-options">
            <div className="aiq-chunk-options-heading">
              <h3>청킹 옵션</h3>
              <p>
                문서를 너무 작게 자르면 답변 근거가 끊기고, 너무 크게 자르면
                검색 정확도가 떨어질 수 있어요. 기본값은 일반 고객센터 문서에
                맞춘 값입니다.
              </p>
            </div>
            <div className="aiq-chunk-option-grid">
              <label className="aiq-field">
                <span>청크 최대 길이</span>
                <input
                  type="number"
                  min="300"
                  max="4000"
                  step="100"
                  value={chunkMaxLength}
                  onChange={(event) =>
                    handleChunkMaxLengthChange(event.target.value)
                  }
                />
                <small>
                  한 조각에 담을 최대 글자 수입니다. 값이 작으면 짧은 질문에 잘
                  맞고, 값이 크면 문맥을 더 오래 유지합니다.
                </small>
              </label>
              <label className="aiq-field">
                <span>청크 최소 길이</span>
                <input
                  type="number"
                  min="0"
                  max="1000"
                  step="50"
                  value={chunkMinLength}
                  onChange={(event) =>
                    handleChunkMinLengthChange(event.target.value)
                  }
                />
                <small>
                  이보다 짧은 청크는 앞뒤 청크와 자동으로 합쳐집니다. 특히 문단
                  분리를 켰을 때 문맥 없는 한 줄짜리 청크가 생기는 걸
                  막아줍니다.
                </small>
              </label>
              <label className="aiq-field">
                <span>청크 겹침 길이</span>
                <input
                  type="number"
                  min="0"
                  max="800"
                  step="50"
                  value={chunkOverlapLength}
                  onChange={(event) =>
                    handleChunkOverlapLengthChange(event.target.value)
                  }
                />
                <small>
                  긴 문단을 나눌 때 앞 청크 끝부분을 다음 청크에 다시 붙이는
                  길이입니다. 경계에서 답변 근거가 끊기는 문제를 줄입니다.
                </small>
              </label>
              <label className="aiq-field aiq-toggle-field">
                <span>분리 기준</span>
                <span className="aiq-toggle-row">
                  <input
                    type="checkbox"
                    checked={splitByMarkdownHeading}
                    onChange={(event) =>
                      handleSplitByMarkdownHeadingChange(event.target.checked)
                    }
                  />
                  <strong>제목(#) 먼저 나누기</strong>
                </span>
                <small>
                  문서에 `#`로 시작하는 마크다운 제목 줄이 있을 때만 효과가
                  있습니다. 제목 줄이 없으면 켜고 꺼도 결과가 같습니다.
                </small>
              </label>
              <label className="aiq-field aiq-toggle-field">
                <span>문단 분리</span>
                <span className="aiq-toggle-row">
                  <input
                    type="checkbox"
                    checked={splitByParagraph}
                    onChange={(event) =>
                      handleSplitByParagraphChange(event.target.checked)
                    }
                  />
                  <strong>문단(빈 줄)마다 청크 나누기</strong>
                </span>
                <small>
                  켜면 빈 줄로 구분된 문단을 서로 합치지 않고 문단 하나당 청크
                  하나로 만듭니다. 끄면 청크 최대 길이까지 여러 문단을
                  이어붙입니다.
                </small>
              </label>
            </div>
          </div>

          <div className="aiq-chunk-preview">
            <div className="aiq-chunk-options-heading">
              <h3>예상 청킹 미리보기</h3>
              <p>
                실제로 임베딩되기 전에, 위 옵션으로 나누면 몇 개의 조각으로
                쪼개지는지 미리 확인할 수 있어요.
              </p>
            </div>
            {chunkPreviewLoading ? (
              <div className="aiq-empty">청킹 결과를 계산하는 중...</div>
            ) : chunkPreviewError ? (
              <div className="aiq-alert">{chunkPreviewError}</div>
            ) : !chunkPreview ? (
              <div className="aiq-empty">
                {knowledgeInputMode === "text"
                  ? "내용을 입력하면 예상 청크가 여기에 표시됩니다."
                  : "파일을 선택하면 예상 청크가 여기에 표시됩니다."}
              </div>
            ) : (
              <>
                <div className="aiq-chunk-preview-summary">
                  <span>총 {chunkPreview.totalLength.toLocaleString()}자</span>
                  <span>{chunkPreview.chunkCount}개 청크로 분리 예상</span>
                </div>
                <div className="aiq-chunk-list">
                  {chunkPreview.chunks.map((chunk) => (
                    <div className="aiq-chunk" key={chunk.chunkIndex}>
                      <strong>#{chunk.chunkIndex + 1}</strong>
                      <span>{chunk.length.toLocaleString()}자</span>
                      <p>{chunk.content}</p>
                    </div>
                  ))}
                </div>
              </>
            )}
          </div>
        </section>

        <section className="aiq-panel">
          <div className="aiq-panel-heading compact">
            <h2>업로드 문서 목록</h2>
            <span>총 {documents.length}개</span>
          </div>
          {documents.length === 0 ? (
            <div className="aiq-empty">업로드한 지식 문서가 없습니다.</div>
          ) : (
            <div className="aiq-document-list">
              {documents.map((document) => (
                <article className="aiq-document-item" key={document.id}>
                  <div className="aiq-document-main">
                    <div className="aiq-document-title-row">
                      <h3>{document.title}</h3>
                      <span
                        className={`aiq-status ${document.status.toLowerCase()}`}
                      >
                        {STATUS_LABEL[document.status] || document.status}
                      </span>
                    </div>
                    <div className="aiq-document-meta">
                      <span>{document.category}</span>
                      <span>
                        {formatKnowledgeSourceName(document.originalFilename)}
                      </span>
                      <span>{document.chunkCount} chunks</span>
                      <span>최대 {document.chunkMaxLength || 1200}자</span>
                      <span>최소 {document.chunkMinLength ?? 200}자</span>
                      <span>겹침 {document.chunkOverlapLength || 150}자</span>
                      <span>
                        {formatSplitMode(
                          document.splitByMarkdownHeading ?? true,
                          document.splitByParagraph ?? false,
                        )}
                      </span>
                      <span>{formatDateTime(document.createdAt)}</span>
                    </div>
                    <details>
                      <summary>청크 미리보기</summary>
                      <div className="aiq-chunk-list">
                        {(document.chunks || []).map((chunk) => (
                          <div className="aiq-chunk" key={chunk.id}>
                            <strong>#{chunk.chunkIndex + 1}</strong>
                            <span>{chunk.qdrantPointId}</span>
                            <p>{chunk.content}</p>
                          </div>
                        ))}
                      </div>
                    </details>
                  </div>
                  <div className="aiq-document-actions">
                    <button
                      type="button"
                      className="aiq-btn aiq-btn-secondary"
                      onClick={() => reembedDocument(document.id)}
                      disabled={Boolean(busyKey)}
                    >
                      재임베딩
                    </button>
                    {document.active && (
                      <button
                        type="button"
                        className="aiq-btn aiq-btn-danger"
                        onClick={() => disableDocument(document.id)}
                        disabled={Boolean(busyKey)}
                      >
                        비활성화
                      </button>
                    )}
                  </div>
                </article>
              ))}
            </div>
          )}
        </section>
      </div>
    </AdminLayout>
  );
}

export default CustomerServiceAiQualityPage;
