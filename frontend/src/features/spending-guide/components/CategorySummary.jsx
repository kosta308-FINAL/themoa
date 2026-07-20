import { useMemo } from "react";
import { Link } from "react-router-dom";
import DashboardIcon from "../../../components/common/DashboardIcon";
import { formatShortDate, formatWon, toNumber } from "../spendingGuideUtils";
import { EmptyState, LoadingState, SectionError } from "./SpendingGuideCommon";

function CategorySummary({ data, error, onNavigate }) {
  const gradient = useMemo(() => {
    if (!data?.items?.length) return "#edf2ef";
    const colors = [
      "#22c55e",
      "#14b8a6",
      "#60a5fa",
      "#f59e0b",
      "#a78bfa",
      "#f472b6",
    ];
    let cursor = 0;
    return `conic-gradient(${data.items
      .map((item, index) => {
        const start = cursor;
        cursor += toNumber(item.percentage);
        return `${colors[index % colors.length]} ${start}% ${cursor}%`;
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
    <>
      <div className="spending-category-cycle">
        <button
          type="button"
          className="spending-cycle-arrow"
          disabled={!data.previousBudgetId}
          onClick={() => onNavigate(data.previousBudgetId)}
          aria-label="이전 급여주기"
        >
          <DashboardIcon name="chevron-left" size={15} />
        </button>
        <div>
          <strong>{Number(data.yearMonth?.slice(5, 7))}월 급여주기</strong>
          <span>
            {formatShortDate(data.cycleStartDate)} ~{" "}
            {data.hasNext ? formatShortDate(data.cycleEndDate) : "오늘"} ·{" "}
            {data.hasNext ? "완료" : "진행 중"}
          </span>
        </div>
        <button
          type="button"
          className="spending-cycle-arrow"
          disabled={!data.nextBudgetId}
          onClick={() => onNavigate(data.nextBudgetId)}
          aria-label="다음 급여주기"
        >
          <DashboardIcon name="chevron-right" size={15} />
        </button>
      </div>
      <div className="spending-category-layout">
        <Link
          className="spending-donut"
          style={{ background: gradient }}
          to={`/dashboard/spending/transactions?budgetId=${data.budgetId}`}
          aria-label="카테고리 소비 상세보기"
        >
          <span>
            <strong>{formatWon(data.positiveNetTotal)}</strong>
            <small>양수 순사용액</small>
          </span>
        </Link>
        <div className="spending-category-legend">
          {data.items.map((item, index) => (
            <Link
              to={`/dashboard/spending/transactions?budgetId=${data.budgetId}&categoryId=${item.categoryId}`}
              key={item.categoryId}
            >
              <i
                style={{
                  background: [
                    "#22c55e",
                    "#14b8a6",
                    "#60a5fa",
                    "#f59e0b",
                    "#a78bfa",
                    "#f472b6",
                  ][index % 6],
                }}
              />
              <span>{item.categoryName}</span>
              <strong>{formatWon(item.amount)}</strong>
              <small>{toNumber(item.percentage)}%</small>
            </Link>
          ))}
        </div>
      </div>
    </>
  );
}

export default CategorySummary;
