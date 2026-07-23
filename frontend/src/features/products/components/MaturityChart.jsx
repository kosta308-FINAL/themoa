// 회사명에서 접미/접두("주식회사"·"(주)")를 떼 짧게. 라벨을 간결하게 보여주기 위함.
const shortCompany = (companyName) =>
  (companyName || "")
    .replace(/주식회사/g, "")
    .replace(/\(주\)/g, "")
    .trim();

// 원 단위를 "만원"으로 반올림 표기. 1842000 → "184만원"
const manwon = (won) => `${Math.round((Number(won) || 0) / 10000)}만원`;

/**
 * 추천 결과 예상 만기금액 막대그래프(시각화 전용, 비교 기능 아님).
 * 추천 순위 순서를 그대로 유지하고, 최댓값을 100%로 정규화해 막대 길이를 정한다.
 * 정기예금(DEPOSIT)처럼 만기금액이 null인 항목은 막대 대신 안내 문구로 표시한다.
 */
function MaturityChart({ items }) {
  const amounts = items
    .map((item) => item.maturityAmountWon)
    .filter((value) => value != null);
  // 그릴 막대가 하나도 없으면(전부 예금 등) 차트를 숨긴다.
  if (amounts.length === 0) {
    return null;
  }
  const max = Math.max(...amounts);

  return (
    <section className="mchart">
      <h3 className="mchart-title">예상 만기금액 비교 (여력 전액 기준)</h3>
      <ul className="mchart-list">
        {items.map((item, index) => {
          const hasAmount = item.maturityAmountWon != null;
          const width = hasAmount
            ? Math.max(6, (item.maturityAmountWon / max) * 100)
            : 0;
          const label =
            manwon(item.maturityAmountWon) +
            (item.bestRateTerm != null ? ` · ${item.bestRateTerm}개월` : "");
          // 막대가 짧으면 라벨을 안에 넣으면 잘리므로 막대 밖(오른쪽)에 표시한다.
          const labelInside = width >= 45;
          return (
            <li className="mchart-row" key={`${item.company}-${index}`}>
              <div className="mchart-label">
                <span className="mchart-rank">{index + 1}</span>
                <span className="mchart-name">
                  {shortCompany(item.company)} {item.productName}
                </span>
              </div>
              {hasAmount ? (
                <div className="mchart-bar-wrap">
                  <div className="mchart-bar" style={{ width: `${width}%` }}>
                    {labelInside && (
                      <span className="mchart-value in">{label}</span>
                    )}
                  </div>
                  {!labelInside && (
                    <span className="mchart-value out">{label}</span>
                  )}
                </div>
              ) : (
                <div className="mchart-bar-wrap">
                  <span className="mchart-na">예금 (만기금액 계산 제외)</span>
                </div>
              )}
            </li>
          );
        })}
      </ul>
    </section>
  );
}

export default MaturityChart;
