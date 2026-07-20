import { useState } from "react";
import { Link } from "react-router-dom";
import DashboardIcon from "../../../components/common/DashboardIcon";
import {
  formatTime,
  formatWon,
  paymentLabel,
  toNumber,
  transactionAmount,
  transactionVisual,
} from "../spendingGuideUtils";
import { EmptyState, LoadingState, SectionError } from "./SpendingGuideCommon";

function TodayTransactions({ data, error, onExpand, onSelect }) {
  const [expanded, setExpanded] = useState(false);
  if (error) return <SectionError message={error} />;
  if (!data) return <LoadingState />;
  if (!data.items?.length)
    return (
      <EmptyState
        icon="receipt"
        title="아직 표시할 소비내역이 없어요"
        description="소비내역이 생기면 오늘 거래가 여기에 표시됩니다."
      />
    );
  const visibleItems = data.items.slice(0, 5);
  const moreItems = data.items.slice(5, 8);
  const moreCount = Math.max(0, Math.min(8, toNumber(data.totalCount)) - 5);
  const renderTransaction = (transaction) => {
    const visual = transactionVisual(transaction);
    const isRefund = toNumber(transaction.netAmount) < 0;
    const isManual = transaction.source === "MANUAL";
    return (
      <button
        type="button"
        className="spending-transaction"
        key={transaction.id}
        onClick={() => onSelect(transaction.id)}
      >
        <span
          className={`spending-transaction-icon${visual.tone ? ` ${visual.tone}` : ""}`}
        >
          <DashboardIcon name={visual.icon} size={18} />
        </span>
        <span className="spending-transaction-info">
          <span className="spending-transaction-name">
            <strong>
              {transaction.merchantDisplayName || transaction.merchantNameRaw}
            </strong>
            {transaction.canceledAmount > 0 && (
              <em className="spending-tiny-badge cancel">일부 취소됨</em>
            )}
            {isManual && <em className="spending-tiny-badge">직접 입력</em>}
            {isRefund && !transaction.canceledAmount && (
              <em className="spending-tiny-badge">취소</em>
            )}
          </span>
          <span className="spending-transaction-meta">
            {[
              formatTime(transaction.usedAt),
              transaction.categoryName,
              paymentLabel(transaction),
            ]
              .filter(Boolean)
              .join(" · ")}
          </span>
        </span>
        <span
          className={`spending-transaction-amount${isRefund ? " refund" : ""}`}
        >
          <strong>{transactionAmount(transaction.netAmount)}</strong>
          <span>
            {isManual
              ? "직접 입력"
              : isRefund
                ? "취소행"
                : transaction.canceledAmount > 0
                  ? `원 결제 ${formatWon(toNumber(transaction.netAmount) + toNumber(transaction.canceledAmount))}`
                  : "카드 자동수집"}
          </span>
        </span>
      </button>
    );
  };
  const toggleExpanded = async () => {
    if (expanded) {
      setExpanded(false);
      return;
    }
    if (data.items.length < Math.min(8, data.totalCount)) await onExpand();
    setExpanded(true);
  };
  return (
    <>
      <div className="spending-transaction-list">
        {visibleItems.map(renderTransaction)}
        <div className={`spending-today-more${expanded ? " expanded" : ""}`}>
          {moreItems.map(renderTransaction)}
        </div>
      </div>
      <div className="spending-list-footer">
        {moreCount > 0 ? (
          <button
            type="button"
            className="spending-ghost-button"
            aria-expanded={expanded}
            onClick={toggleExpanded}
          >
            {expanded ? "오늘 내역 접기" : `오늘 내역 ${moreCount}건 더보기`}
          </button>
        ) : (
          <span />
        )}
        <Link
          className="spending-link-button"
          to="/dashboard/spending/transactions"
        >
          전체 소비내역 보기 <DashboardIcon name="chevron-right" size={15} />
        </Link>
      </div>
    </>
  );
}

export default TodayTransactions;
