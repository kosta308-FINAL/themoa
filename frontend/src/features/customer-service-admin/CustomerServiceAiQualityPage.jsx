import { useEffect, useMemo, useState } from "react";
import AdminLayout from "../../components/layout/AdminLayout";
import {
  createAdminCustomerKnowledgeText,
  disableAdminCustomerKnowledgeDocument,
  getAdminCustomerAiSettings,
  getAdminCustomerKnowledgeDocuments,
  getAdminCustomerKnowledgeMetadataOptions,
  previewAdminCustomerAiAnswer,
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

function formatHeadingSplit(value) {
  return value ? "제목 기준 분리" : "문단 기준 분리";
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
  const [chunkOverlapLength, setChunkOverlapLength] = useState(150);
  const [splitByMarkdownHeading, setSplitByMarkdownHeading] = useState(true);
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
    Promise.all([
      loadSettings(),
      loadDocuments(),
      loadMetadataOptions(),
    ]).catch((requestError) => {
      setError(
        getApiErrorMessage(
          requestError,
          "AI 품질관리 정보를 불러오지 못했어요.",
        ),
      );
    });
  }, []);

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
        chunkOverlapLength: Number(chunkOverlapLength),
        splitByMarkdownHeading,
      });
      setUploadTitle("");
      setUploadFile(null);
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
    setBusyKey("text");
    setError("");
    setNotice("");
    try {
      await createAdminCustomerKnowledgeText({
        title: uploadTitle,
        category: uploadCategory,
        content: knowledgeText,
        chunkMaxLength: Number(chunkMaxLength),
        chunkOverlapLength: Number(chunkOverlapLength),
        splitByMarkdownHeading,
      });
      setUploadTitle("");
      setKnowledgeText("");
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

        {notice && <div className="aiq-alert success">{notice}</div>}
        {error && <div className="aiq-alert">{error}</div>}

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
                파일을 올리거나 일반 텍스트를 입력하면 청킹 후 고객센터
                Qdrant 컬렉션에 임베딩됩니다.
              </p>
            </div>
          </div>

          <div className="aiq-segmented" aria-label="지식 추가 방식">
            <button
              type="button"
              className={knowledgeInputMode === "text" ? "active" : ""}
              onClick={() => setKnowledgeInputMode("text")}
            >
              직접 입력
            </button>
            <button
              type="button"
              className={knowledgeInputMode === "file" ? "active" : ""}
              onClick={() => setKnowledgeInputMode("file")}
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
                onChange={(event) => setKnowledgeText(event.target.value)}
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
                    setUploadFile(event.target.files?.[0] || null)
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
                  onChange={(event) => setChunkMaxLength(event.target.value)}
                />
                <small>
                  한 조각에 담을 최대 글자 수입니다. 값이 작으면 짧은 질문에
                  잘 맞고, 값이 크면 문맥을 더 오래 유지합니다.
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
                    setChunkOverlapLength(event.target.value)
                  }
                />
                <small>
                  긴 문단을 나눌 때 앞 청크 끝부분을 다음 청크에 다시 붙이는
                  길이입니다. 경계에서 답변 근거가 끊기는 문제를 줄입니다.
                </small>
              </label>
              <label className="aiq-toggle-field">
                <input
                  type="checkbox"
                  checked={splitByMarkdownHeading}
                  onChange={(event) =>
                    setSplitByMarkdownHeading(event.target.checked)
                  }
                />
                <span>
                  <strong>Markdown 제목 기준으로 먼저 나누기</strong>
                  <small>
                    md 파일의 #, ## 제목을 큰 단락 경계로 봅니다. 일반 txt처럼
                    제목 표시가 없는 문서는 문단 기준으로만 묶입니다.
                  </small>
                </span>
              </label>
            </div>
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
                      <span>겹침 {document.chunkOverlapLength || 150}자</span>
                      <span>
                        {formatHeadingSplit(
                          document.splitByMarkdownHeading ?? true,
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
