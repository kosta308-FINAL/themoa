import { useEffect, useState } from "react";
import { getAdminLogFiles } from "../../../api/errorLogApi";
import { getApiErrorMessage } from "../../../utils/apiError";

const LEVEL_OPTIONS = ["ERROR", "WARN", "INFO"];

function levelBadgeClass(level) {
  if (level === "ERROR") return "red";
  if (level === "WARN") return "yellow";
  return "gray";
}

function LogFileViewerPanel() {
  const [level, setLevel] = useState("WARN");
  const [keyword, setKeyword] = useState("");
  const [entries, setEntries] = useState([]);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState("");
  const [expandedIndex, setExpandedIndex] = useState(null);

  const load = async () => {
    setIsLoading(true);
    setError("");
    try {
      const data = await getAdminLogFiles({
        level,
        keyword: keyword || undefined,
        limit: 200,
      });
      setEntries(data || []);
    } catch (requestError) {
      setError(
        getApiErrorMessage(requestError, "로그 파일을 불러오지 못했어요."),
      );
    } finally {
      setIsLoading(false);
    }
  };

  useEffect(() => {
    const timer = window.setTimeout(load, 250);
    return () => window.clearTimeout(timer);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [level, keyword]);

  return (
    <div className="ela-filelog">
      <section className="ela-filters">
        <div className="ela-level-tabs">
          {LEVEL_OPTIONS.map((option) => (
            <button
              key={option}
              type="button"
              className={`ela-level-tab ${level === option ? "active" : ""}`}
              onClick={() => {
                setExpandedIndex(null);
                setLevel(option);
              }}
            >
              {option}
            </button>
          ))}
        </div>
        <input
          type="text"
          className="ela-input"
          placeholder="메시지·logger·traceId 검색"
          value={keyword}
          onChange={(e) => setKeyword(e.target.value)}
        />
        <button
          type="button"
          className="ela-btn ela-btn-primary"
          onClick={load}
        >
          새로고침
        </button>
      </section>

      <section className="ela-panel">
        <div className="ela-panel-header">
          <div>
            <div className="ela-panel-title">파일 로그 ({level})</div>
            <div className="ela-panel-sub">
              서버의 info.log / error.log를 직접 읽어온 최신 로그입니다. 최신순
              최대 200건.
            </div>
          </div>
        </div>
        {error && <div className="ela-alert">{error}</div>}
        {isLoading ? (
          <div className="ela-empty">불러오는 중...</div>
        ) : entries.length === 0 ? (
          <div className="ela-empty">조건에 맞는 로그가 없습니다.</div>
        ) : (
          <ul className="ela-filelog-list">
            {entries.map((entry, index) => {
              const lines = entry.message.split("\n");
              const isMultiline = lines.length > 1;
              const isExpanded = expandedIndex === index;
              return (
                <li
                  key={`${entry.timestamp}-${index}`}
                  className="ela-filelog-item"
                  data-level={entry.level.toLowerCase()}
                >
                  <button
                    type="button"
                    className={`ela-filelog-row ${isMultiline ? "" : "ela-filelog-row-plain"}`}
                    onClick={() =>
                      isMultiline && setExpandedIndex(isExpanded ? null : index)
                    }
                  >
                    <span className="ela-filelog-chevron">
                      {isMultiline ? (isExpanded ? "▾" : "▸") : ""}
                    </span>
                    <span
                      className={`ela-badge ${levelBadgeClass(entry.level)}`}
                    >
                      {entry.level}
                    </span>
                    <span className="ela-filelog-time">{entry.timestamp}</span>
                    <span className="ela-filelog-logger" title={entry.logger}>
                      {entry.logger}
                    </span>
                    <span className="ela-filelog-message">{lines[0]}</span>
                  </button>
                  {isExpanded && isMultiline && (
                    <pre className="ela-filelog-detail">{entry.message}</pre>
                  )}
                </li>
              );
            })}
          </ul>
        )}
      </section>
    </div>
  );
}

export default LogFileViewerPanel;
