import { useEffect, useMemo, useState } from "react";
import MarkdownContent from "../../../components/common/MarkdownContent";
import { getFaqs, putFaqFeedback } from "../../../api/customerServiceApi";
import { getApiErrorMessage } from "../../../utils/apiError";

const CATEGORY_EMOJI = {
  CARD_SYNC: "💳",
  DAILY_BUDGET: "🎯",
  FIXED_EXPENSE: "🔄",
  MANUAL_EXPENSE: "✏️",
  ACCOUNT_SECURITY: "🔒",
  POLICY_PRODUCT: "🏛️",
};

function FaqPanel({ searchTerm }) {
  const [items, setItems] = useState([]);
  const [categories, setCategories] = useState([]);
  const [selectedCategory, setSelectedCategory] = useState("all");
  const [openFaqId, setOpenFaqId] = useState(null);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState("");
  const [feedbackPending, setFeedbackPending] = useState({});

  const load = async () => {
    setIsLoading(true);
    setError("");
    try {
      const data = await getFaqs({
        categoryCode: selectedCategory === "all" ? undefined : selectedCategory,
        keyword: searchTerm || undefined,
        size: 100,
      });
      const nextItems = data?.items || [];
      setItems(nextItems);
      setCategories((prev) =>
        prev.length > 0 ? prev : deriveCategories(nextItems),
      );
      if (nextItems.length > 0 && openFaqId === null) {
        setOpenFaqId(nextItems[0].id);
      }
    } catch (requestError) {
      setError(getApiErrorMessage(requestError, "FAQ를 불러오지 못했어요."));
    } finally {
      setIsLoading(false);
    }
  };

  useEffect(() => {
    const timer = window.setTimeout(() => {
      load();
    }, 250);
    return () => window.clearTimeout(timer);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [selectedCategory, searchTerm]);

  const deriveCategories = (list) => {
    const seen = new Map();
    list.forEach((item) => {
      if (!seen.has(item.categoryCode)) {
        seen.set(item.categoryCode, item.categoryName);
      }
    });
    return Array.from(seen.entries()).map(([code, name]) => ({ code, name }));
  };

  const toggleFaq = (id) => {
    setOpenFaqId((prev) => (prev === id ? null : id));
  };

  const handleFeedback = async (faq, helpful) => {
    if (feedbackPending[faq.id]) return;
    setFeedbackPending((prev) => ({ ...prev, [faq.id]: true }));
    const prevItems = items;
    setItems((prev) =>
      prev.map((item) => {
        if (item.id !== faq.id) return item;
        const wasSame = item.myFeedback === helpful;
        let helpfulCount = item.helpfulCount;
        let unhelpfulCount = item.unhelpfulCount;
        if (!wasSame) {
          if (item.myFeedback === true) helpfulCount -= 1;
          if (item.myFeedback === false) unhelpfulCount -= 1;
          if (helpful) helpfulCount += 1;
          else unhelpfulCount += 1;
        }
        return { ...item, myFeedback: helpful, helpfulCount, unhelpfulCount };
      }),
    );
    try {
      await putFaqFeedback(faq.id, helpful);
    } catch {
      setItems(prevItems);
    } finally {
      setFeedbackPending((prev) => ({ ...prev, [faq.id]: false }));
    }
  };

  const categoryChips = useMemo(
    () => [{ code: "all", name: "전체보기" }, ...categories],
    [categories],
  );

  return (
    <div>
      <div className="faq-categories">
        {categoryChips.map((cat) => (
          <button
            key={cat.code}
            type="button"
            className={`cat-btn ${selectedCategory === cat.code ? "active" : ""}`}
            onClick={() => setSelectedCategory(cat.code)}
          >
            {cat.code === "all"
              ? cat.name
              : `${CATEGORY_EMOJI[cat.code] || ""} ${cat.name}`}
          </button>
        ))}
      </div>

      {error && <div className="cs-inline-error">{error}</div>}

      <div className="faq-list">
        {isLoading && <div className="faq-empty">불러오는 중...</div>}
        {!isLoading && items.length === 0 && !error && (
          <div className="faq-empty">
            {searchTerm
              ? "검색 결과와 일치하는 FAQ 질문이 없습니다."
              : "등록된 FAQ가 없습니다."}
          </div>
        )}
        {!isLoading &&
          items.map((item) => {
            const isOpen = openFaqId === item.id;
            return (
              <div key={item.id} className={`faq-item ${isOpen ? "open" : ""}`}>
                <button
                  type="button"
                  className="faq-question"
                  onClick={() => toggleFaq(item.id)}
                >
                  <span className="q-badge">{item.categoryName}</span>
                  <span className="q-title">{item.question}</span>
                  <svg
                    className="chevron"
                    width="18"
                    height="18"
                    viewBox="0 0 24 24"
                    fill="none"
                    stroke="currentColor"
                    strokeWidth="2"
                    strokeLinecap="round"
                    strokeLinejoin="round"
                  >
                    <path d="m6 9 6 6 6-6" />
                  </svg>
                </button>
                <div className="faq-answer">
                  <div className="faq-answer-inner">
                    <MarkdownContent
                      markdown={item.answerMarkdown}
                      className="faq-markdown"
                    />
                    <div className="faq-feedback">
                      <span>
                        이 답변이 도움이 되었나요? ({item.helpfulCount}명에게
                        도움됨)
                      </span>
                      <div className="feedback-btns">
                        <button
                          type="button"
                          className={`feedback-btn ${item.myFeedback === true ? "selected" : ""}`}
                          onClick={() => handleFeedback(item, true)}
                        >
                          👍 예
                        </button>
                        <button
                          type="button"
                          className={`feedback-btn ${item.myFeedback === false ? "selected" : ""}`}
                          onClick={() => handleFeedback(item, false)}
                        >
                          👎 아니오
                        </button>
                      </div>
                    </div>
                  </div>
                </div>
              </div>
            );
          })}
      </div>
    </div>
  );
}

export default FaqPanel;
