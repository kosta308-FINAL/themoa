import { useEffect, useMemo, useState } from "react";
import AdminLayout from "../../components/layout/AdminLayout";
import {
  disableAdminCustomerKnowledgeDocument,
  getAdminCustomerAiSettings,
  getAdminCustomerKnowledgeDocuments,
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

function CustomerServiceAiQualityPage() {
  const [query, setQuery] = useState("처음 카드동기화 했는데 1분넘도록 동기화가 안됨");
  const [topK, setTopK] = useState(6);
  const [minimumSimilarity, setMinimumSimilarity] = useState(0.45);
  const [systemPrompt, setSystemPrompt] = useState("");
  const [searchResult, setSearchResult] = useState(null);
  const [answerPreview, setAnswerPreview] = useState("");
  const [documents, setDocuments] = useState([]);
  const [uploadTitle, setUploadTitle] = useState("");
  const [uploadCategory, setUploadCategory] = useState("고객센터 운영정책");
  const [uploadFile, setUploadFile] = useState(null);
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

  useEffect(() => {
    Promise.all([loadSettings(), loadDocuments()]).catch((requestError) => {
      setError(
        getApiErrorMessage(requestError, "AI 품질관리 정보를 불러오지 못했어요."),
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
      setError(getApiErrorMessage(requestError, "Qdrant 검색 테스트에 실패했어요."));
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
      setError(getApiErrorMessage(requestError, "운영 기본값 저장에 실패했어요."));
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
      });
      setUploadTitle("");
      setUploadFile(null);
      event.target.reset();
      await loadDocuments();
      setNotice("문서를 청킹하고 Qdrant에 임베딩했어요.");
    } catch (requestError) {
      setError(getApiErrorMessage(requestError, "문서 업로드 또는 임베딩에 실패했어요."));
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
      setNotice("문서를 비활성화했어요. 다음 검색부터 컨텍스트 후보에서 제외됩니다.");
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
              <p>관리자가 입력한 질문으로 검색 점수와 최종 답변을 함께 확인합니다.</p>
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
                  <article className="aiq-result-item" key={`${item.sourceType}-${item.sourceId}-${item.rank}`}>
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
              <div className="aiq-empty">답변까지 실행하면 여기에 결과가 표시됩니다.</div>
            )}
          </div>
        </section>

        <section className="aiq-panel">
          <div className="aiq-panel-heading">
            <div>
              <h2>md/txt 지식 문서 업로드</h2>
              <p>업로드한 문서는 청킹 후 고객센터 Qdrant 컬렉션에 임베딩됩니다.</p>
            </div>
          </div>
          <form className="aiq-upload-form" onSubmit={uploadDocument}>
            <input
              type="text"
              placeholder="문서 제목"
              value={uploadTitle}
              onChange={(event) => setUploadTitle(event.target.value)}
            />
            <input
              type="text"
              placeholder="카테고리"
              value={uploadCategory}
              onChange={(event) => setUploadCategory(event.target.value)}
            />
            <input
              type="file"
              accept=".md,.txt,text/markdown,text/plain"
              onChange={(event) => setUploadFile(event.target.files?.[0] || null)}
            />
            <button
              type="submit"
              className="aiq-btn aiq-btn-primary"
              disabled={Boolean(busyKey)}
            >
              청킹 후 임베딩
            </button>
          </form>
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
                      <span className={`aiq-status ${document.status.toLowerCase()}`}>
                        {STATUS_LABEL[document.status] || document.status}
                      </span>
                    </div>
                    <div className="aiq-document-meta">
                      <span>{document.category}</span>
                      <span>{document.originalFilename}</span>
                      <span>{document.chunkCount} chunks</span>
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
