import { useEffect, useState } from "react";
import { getAdminApiPerformance } from "../../../api/errorLogApi";
import { getApiErrorMessage } from "../../../utils/apiError";

function speedBadgeClass(avgMs) {
  if (avgMs >= 500) return "red";
  if (avgMs >= 150) return "yellow";
  return "green";
}

function speedBarColor(avgMs) {
  if (avgMs >= 500) return "var(--red)";
  if (avgMs >= 150) return "var(--orange)";
  return "var(--green)";
}

function ApiPerformancePanel() {
  const [keyword, setKeyword] = useState("");
  const [stats, setStats] = useState([]);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState("");

  const load = async () => {
    setIsLoading(true);
    setError("");
    try {
      const data = await getAdminApiPerformance({
        keyword: keyword || undefined,
      });
      setStats(data || []);
    } catch (requestError) {
      setError(
        getApiErrorMessage(requestError, "API 성능 통계를 불러오지 못했어요."),
      );
    } finally {
      setIsLoading(false);
    }
  };

  useEffect(() => {
    const timer = window.setTimeout(load, 250);
    return () => window.clearTimeout(timer);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [keyword]);

  const top5 = stats.slice(0, 5);
  const maxAvgMs = top5.length > 0 ? top5[0].avgMs : 0;

  return (
    <div className="ela-apiperf">
      <section className="ela-filters">
        <input
          type="text"
          className="ela-input"
          placeholder="URI·Method 검색"
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

      {error && <div className="ela-alert">{error}</div>}

      {!isLoading && top5.length > 0 && (
        <section className="ela-panel ela-apiperf-top-panel">
          <div className="ela-panel-header">
            <div>
              <div className="ela-panel-title">가장 느린 API TOP 5</div>
              <div className="ela-panel-sub">
                평균 응답시간이 가장 긴 엔드포인트 순입니다.
              </div>
            </div>
          </div>
          <ol className="ela-apiperf-top-list">
            {top5.map((stat, index) => (
              <li
                key={`top-${stat.method}-${stat.uri}-${stat.status}-${index}`}
                className="ela-apiperf-top-item"
              >
                <span className="ela-apiperf-top-rank">{index + 1}</span>
                <div className="ela-apiperf-top-main">
                  <div className="ela-apiperf-top-uri">
                    <span className="ela-apiperf-top-method">
                      {stat.method}
                    </span>
                    <span title={stat.uri}>{stat.uri}</span>
                  </div>
                  <div className="ela-apiperf-top-bar-track">
                    <div
                      className="ela-apiperf-top-bar"
                      style={{
                        width: `${maxAvgMs === 0 ? 0 : (stat.avgMs / maxAvgMs) * 100}%`,
                        background: speedBarColor(stat.avgMs),
                      }}
                    />
                  </div>
                </div>
                <span className={`ela-badge ${speedBadgeClass(stat.avgMs)}`}>
                  {stat.avgMs.toFixed(1)}ms
                </span>
              </li>
            ))}
          </ol>
        </section>
      )}

      <section className="ela-panel">
        <div className="ela-panel-header">
          <div>
            <div className="ela-panel-title">전체 엔드포인트 목록</div>
            <div className="ela-panel-sub">
              서버 실행 후 누적된 값입니다(재시작 시 초기화). DB에 요청마다
              저장하지 않고 메모리 집계만 읽어옵니다. 평균 응답시간 내림차순.
            </div>
          </div>
        </div>
        {isLoading ? (
          <div className="ela-empty">불러오는 중...</div>
        ) : stats.length === 0 ? (
          <div className="ela-empty">
            아직 집계된 요청이 없습니다. API를 몇 번 호출한 뒤 새로고침 해
            보세요.
          </div>
        ) : (
          <table className="ela-table">
            <thead>
              <tr>
                <th>Method</th>
                <th>URI</th>
                <th>상태코드</th>
                <th>호출수</th>
                <th>평균(ms)</th>
                <th>최대(ms)</th>
              </tr>
            </thead>
            <tbody>
              {stats.map((stat, index) => (
                <tr key={`${stat.method}-${stat.uri}-${stat.status}-${index}`}>
                  <td>{stat.method}</td>
                  <td className="ela-table-uri" title={stat.uri}>
                    {stat.uri}
                  </td>
                  <td>{stat.status}</td>
                  <td>{stat.count}</td>
                  <td>
                    <span
                      className={`ela-badge ${speedBadgeClass(stat.avgMs)}`}
                    >
                      {stat.avgMs.toFixed(1)}
                    </span>
                  </td>
                  <td>{stat.maxMs.toFixed(1)}</td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </section>
    </div>
  );
}

export default ApiPerformancePanel;
