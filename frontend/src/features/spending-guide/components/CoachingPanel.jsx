import { useState } from "react";
import DashboardIcon from "../../../components/common/DashboardIcon";
import { formatWon, toNumber } from "../spendingGuideUtils";
import {
  EmptyState,
  LoadingState,
  PanelTitle,
  SectionError,
} from "./SpendingGuideCommon";

function coachingCardContent(card) {
  const label = `${card.targetLabel || ""} ${card.title || ""}`;
  const visual = /카페|간식/.test(label)
    ? { icon: "coffee", tone: "green" }
    : /배달|식비|외식/.test(label)
      ? { icon: "utensils", tone: "orange" }
      : /교통|택시|주유|차량/.test(label)
        ? { icon: "car", tone: "blue" }
        : /편의점|마트|쇼핑/.test(label)
          ? { icon: "bag", tone: "orange" }
          : { icon: "sparkle", tone: "purple" };
  const sentences =
    String(card.body || "")
      .match(/[^.!?]+[.!?]?/g)
      ?.map((sentence) => sentence.trim())
      .filter(Boolean) || [];
  const savingIndex = sentences.findIndex((sentence) =>
    /아낄|절감/.test(sentence),
  );

  if (savingIndex >= 0) {
    return {
      ...visual,
      body: sentences.filter((_, index) => index !== savingIndex).join(" "),
      saving: sentences[savingIndex],
    };
  }
  return {
    ...visual,
    body: card.body,
    saving:
      toNumber(card.estimatedSaving) > 0
        ? `예상 절감액 ${formatWon(card.estimatedSaving)}`
        : "",
  };
}

function CoachingCards({ data, error, onDismiss, pendingId, expanded }) {
  if (error) return <SectionError message={error} />;
  if (!data) return <LoadingState />;
  if (!data.items?.length)
    return (
      <EmptyState
        icon="sparkle"
        title="아직 제공할 소비 코칭이 없어요"
        description={
          data.emptyReason === "CARD_NOT_CONNECTED"
            ? "카드를 연결하면 소비 습관을 분석해드려요."
            : "분석 가능한 소비내역이 쌓이면 맞춤 코칭을 보여드려요."
        }
      />
    );
  return (
    <div
      id="spending-coaching-list"
      className={`spending-coaching-list${expanded ? " expanded" : ""}`}
    >
      {data.items.map((card, index) => {
        const content = coachingCardContent(card);
        return (
          <article className={index === 0 ? "active" : ""} key={card.id}>
            <span className={`spending-coach-icon ${content.tone}`}>
              <DashboardIcon name={content.icon} size={18} />
            </span>
            <h3>{card.title}</h3>
            {content.body && <p>{content.body}</p>}
            {content.saving && <strong>{content.saving}</strong>}
            <div className="spending-coach-actions">
              <button
                type="button"
                disabled={pendingId === card.id}
                onClick={() => onDismiss(card.id, "NOT_WASTE")}
              >
                필요한 소비
              </button>
              <button
                type="button"
                disabled={pendingId === card.id}
                onClick={() => onDismiss(card.id, "HIDE")}
              >
                그만 보기
              </button>
            </div>
          </article>
        );
      })}
    </div>
  );
}

function CoachingPanel({ data, error, onDismiss, pendingId }) {
  const [expanded, setExpanded] = useState(false);
  const itemCount = data?.items?.length || 0;
  const isExpanded = expanded && itemCount > 1;

  return (
    <section className="spending-panel spending-coaching-panel">
      <div className="spending-panel-head">
        <PanelTitle
          icon="sparkle"
          title="이번 달 이렇게 아껴봐요"
          description="지난 소비 습관을 바탕으로 알려드려요"
          tone="purple"
        />
        {itemCount > 1 && (
          <button
            type="button"
            className="spending-coach-expand"
            aria-expanded={isExpanded}
            aria-controls="spending-coaching-list"
            onClick={() => setExpanded((current) => !current)}
          >
            {isExpanded ? "접기" : `${itemCount}개 펼치기`}
            <span aria-hidden="true" />
          </button>
        )}
      </div>
      <CoachingCards
        data={data}
        error={error}
        onDismiss={onDismiss}
        pendingId={pendingId}
        expanded={isExpanded}
      />
    </section>
  );
}

export default CoachingPanel;

