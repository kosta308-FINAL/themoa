const PART_LABELS = {
  savings: "예·적금",
  loans: "대출",
};

const number = (value) => Number(value ?? 0).toLocaleString();

/**
 * 수집 결과. savings/loans는 각각 따로 성공·실패하므로 파트별로 나눠서 보여준다.
 */
function CollectResultTable({ result }) {
  const parts = Object.keys(PART_LABELS).filter((key) => result?.[key]);

  if (parts.length === 0) {
    return null;
  }

  return (
    <table className="fa-table">
      <thead>
        <tr>
          <th>구분</th>
          <th>결과</th>
          <th>조회</th>
          <th>신규</th>
          <th>갱신</th>
          <th>판매종료 제외</th>
        </tr>
      </thead>
      <tbody>
        {parts.map((key) => {
          const part = result[key];
          return (
            <tr key={key}>
              <td>{PART_LABELS[key]}</td>
              <td>
                <span
                  className={`fa-badge ${part.success ? "fa-badge-ok" : "fa-badge-fail"}`}
                >
                  {part.success ? "성공" : "실패"}
                </span>
              </td>
              <td>{number(part.fetched)}</td>
              <td className={part.inserted > 0 ? "fa-em" : ""}>
                {number(part.inserted)}
              </td>
              <td>{number(part.updated)}</td>
              <td>{number(part.skippedClosed)}</td>
            </tr>
          );
        })}
      </tbody>
    </table>
  );
}

export default CollectResultTable;
