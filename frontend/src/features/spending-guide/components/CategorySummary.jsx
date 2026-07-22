import { useMemo } from "react";
import { formatWon, toNumber } from "../spendingGuideUtils";
import { EmptyState, LoadingState, SectionError } from "./SpendingGuideCommon";

// 인접 색상 간 구분이 최대가 되도록 고정된 순서의 카테고리 팔레트(색약 대비 검증됨)
const CATEGORY_COLORS = [
  "#2a78d6", // blue
  "#008300", // green
  "#e87ba4", // magenta
  "#eda100", // yellow
  "#1baf7a", // aqua
  "#eb6834", // orange
  "#4a3aa7", // violet
  "#e34948", // red
];

function CategorySummary({ data, error }) {
  const gradient = useMemo(() => {
    if (!data?.items?.length) return "#edf2ef";
    let cursor = 0;
    return `conic-gradient(${data.items
      .map((item, index) => {
        const start = cursor;
        cursor += toNumber(item.percentage);
        return `${CATEGORY_COLORS[index % CATEGORY_COLORS.length]} ${start}% ${cursor}%`;
      })
      .join(", ")})`;
  }, [data]);
  if (error) return <SectionError message={error} />;
  if (!data) return <LoadingState />;
  if (!data.items?.length)
    return (
      <EmptyState
        icon="chart"
        title="분석할 카테고리 데이터가 없어요"
        description="소비내역이 연결되면 카테고리별 비중이 표시됩니다."
      />
    );
  return (
    <div className="spending-category-layout">
      <div className="spending-donut" style={{ background: gradient }}>
        <span>
          <strong>{formatWon(data.positiveNetTotal)}</strong>
          <small>양수 순사용액</small>
        </span>
      </div>
      <div className="spending-category-legend">
        {data.items.map((item, index) => (
          <div key={item.categoryId}>
            <i
              style={{
                background: CATEGORY_COLORS[index % CATEGORY_COLORS.length],
              }}
            />
            <span>{item.categoryName}</span>
            <strong>{formatWon(item.amount)}</strong>
            <small>{toNumber(item.percentage)}%</small>
          </div>
        ))}
      </div>
    </div>
  );
}

export default CategorySummary;
