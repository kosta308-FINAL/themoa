import { useState } from "react";
import { Link, useSearchParams } from "react-router-dom";
import DashboardIcon from "../../components/common/DashboardIcon";
import MarkdownContent from "../../components/common/MarkdownContent";
import { askCustomerServiceChat } from "../../api/customerServiceApi";
import FaqPanel from "./components/FaqPanel";
import InquiryForm from "./components/InquiryForm";
import MyInquiryList from "./components/MyInquiryList";
import "./CustomerServicePage.css";

const QUICK_TAGS = [
  "카드연동",
  "하루권장액",
  "고정지출",
  "수기지출",
  "비밀번호",
];

function CustomerServicePage() {
  const [searchParams, setSearchParams] = useSearchParams();
  const [activeTab, setActiveTab] = useState(searchParams.get("tab") || "faq");
  const [searchMode, setSearchMode] = useState("chat");
  const [searchTerm, setSearchTerm] = useState("");
  const [chatAnswer, setChatAnswer] = useState(null);
  const [chatError, setChatError] = useState("");
  const [isChatLoading, setIsChatLoading] = useState(false);
  const [myListRefreshKey, setMyListRefreshKey] = useState(0);
  const [focusInquiryId] = useState(searchParams.get("inquiryId"));
  const [showSuccessModal, setShowSuccessModal] = useState(false);

  const switchTab = (tab) => {
    setActiveTab(tab);
    setSearchParams(tab === "faq" ? {} : { tab });
  };

  const handleSearchTag = (tag) => {
    setSearchTerm(tag);
    if (searchMode === "chat") {
      handleChatSubmit(tag);
      return;
    }
    switchTab("faq");
  };

  const handleHeroSearch = () => {
    if (searchMode === "chat") {
      handleChatSubmit(searchTerm);
      return;
    }
    switchTab("faq");
  };

  const handleChatSubmit = async (message) => {
    const normalized = (message || "").trim();
    if (!normalized || isChatLoading) return;
    setChatError("");
    setIsChatLoading(true);
    try {
      const response = await askCustomerServiceChat({
        message: normalized,
        conversationId: chatAnswer?.conversationId || null,
      });
      setChatAnswer(response);
    } catch (requestError) {
      setChatError(
        "챗봇 답변을 불러오지 못했어요. 잠시 후 다시 시도해 주세요.",
      );
    } finally {
      setIsChatLoading(false);
    }
  };

  const handleInquirySubmitted = () => {
    setShowSuccessModal(true);
    setMyListRefreshKey((prev) => prev + 1);
  };

  const handleCloseModal = () => {
    setShowSuccessModal(false);
    switchTab("myList");
  };

  return (
    <div className="cs-page">
      {/* HERO BANNER */}
      <section className="cs-hero">
        <div className="cs-hero-content">
          <div className="cs-hero-badge">
            <svg
              className="icon icon-sm"
              viewBox="0 0 24 24"
              width="15"
              height="15"
              fill="none"
              stroke="currentColor"
              strokeWidth="2"
              strokeLinecap="round"
              strokeLinejoin="round"
            >
              <path d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z" />
            </svg>
            24시간 열려있는 더모아 지원팀
          </div>
          <h1>무엇을 도와드릴까요?</h1>
          <p>
            챗봇에게 먼저 물어보거나, 일반검색으로 FAQ를 빠르게 찾아보세요.
            해결되지 않은 문제는 1:1 문의로 이어서 도와드리겠습니다.
          </p>

          <div className="cs-search-mode" aria-label="고객센터 검색 모드">
            <button
              type="button"
              className={`cs-mode-btn ${searchMode === "chat" ? "active" : ""}`}
              onClick={() => setSearchMode("chat")}
            >
              챗봇
            </button>
            <button
              type="button"
              className={`cs-mode-btn ${searchMode === "faq" ? "active" : ""}`}
              onClick={() => setSearchMode("faq")}
            >
              일반검색
            </button>
          </div>

          <div className="cs-search-box">
            <DashboardIcon name="search" size={18} />
            <input
              type="text"
              placeholder={
                searchMode === "chat"
                  ? "궁금한 내용을 문장으로 물어보세요 (예: 정책 추천은 지역을 꼭 입력해야 하나요?)"
                  : "궁금한 답변이나 키워드를 검색해보세요 (예: 카드연동, 하루권장액, 수기지출)"
              }
              value={searchTerm}
              onChange={(e) => setSearchTerm(e.target.value)}
              onKeyDown={(e) => {
                if (e.key === "Enter") handleHeroSearch();
              }}
            />
            <button
              type="button"
              className="cs-search-btn"
              onClick={handleHeroSearch}
              disabled={isChatLoading}
            >
              {searchMode === "chat"
                ? isChatLoading
                  ? "답변 중..."
                  : "질문하기"
                : "검색하기"}
            </button>
          </div>

          <div className="quick-tags">
            <span>인기 키워드:</span>
            {QUICK_TAGS.map((tag) => (
              <button
                key={tag}
                type="button"
                className="tag-chip"
                onClick={() => handleSearchTag(tag)}
              >
                #{tag}
              </button>
            ))}
          </div>

          {searchMode === "chat" &&
            (chatAnswer || chatError || isChatLoading) && (
              <div className="cs-chat-result">
                {isChatLoading && (
                  <div className="cs-chat-loading">
                    고객센터 지식에서 답변을 찾고 있어요.
                  </div>
                )}
                {chatError && (
                  <div className="cs-inline-error">{chatError}</div>
                )}
                {chatAnswer && !isChatLoading && (
                  <>
                    <div className="cs-chat-answer-head">
                      <span>AI 고객센터 답변</span>
                      {chatAnswer.needsHumanSupport && (
                        <button
                          type="button"
                          className="cs-chat-link-btn"
                          onClick={() => switchTab("inquire")}
                        >
                          1:1 문의로 이어가기
                        </button>
                      )}
                    </div>
                    <MarkdownContent
                      markdown={chatAnswer.answerMarkdown}
                      className="cs-chat-answer-body"
                    />
                    {chatAnswer.citations?.length > 0 && (
                      <div className="cs-chat-citations">
                        <span>참고한 고객센터 지식</span>
                        <div>
                          {chatAnswer.citations.map((citation) => (
                            <button
                              key={`${citation.sourceType}-${citation.sourceId}`}
                              type="button"
                              className="cs-citation-chip"
                              onClick={() => {
                                setSearchMode("faq");
                                setSearchTerm(citation.title);
                                switchTab("faq");
                              }}
                            >
                              {citation.title}
                            </button>
                          ))}
                        </div>
                      </div>
                    )}
                  </>
                )}
              </div>
            )}
        </div>
      </section>

      {/* TABS BAR */}
      <div className="cs-tabs-bar">
        <button
          type="button"
          className={`cs-tab-btn ${activeTab === "faq" ? "active" : ""}`}
          onClick={() => switchTab("faq")}
        >
          <svg
            width="18"
            height="18"
            viewBox="0 0 24 24"
            fill="none"
            stroke="currentColor"
            strokeWidth="2"
            strokeLinecap="round"
            strokeLinejoin="round"
          >
            <circle cx="12" cy="12" r="10" />
            <path d="M9.09 9a3 3 0 0 1 5.83 1c0 2-3 3-3 3" />
            <path d="M12 17h.01" />
          </svg>
          자주 묻는 질문 (FAQ)
        </button>
        <button
          type="button"
          className={`cs-tab-btn ${activeTab === "inquire" ? "active" : ""}`}
          onClick={() => switchTab("inquire")}
        >
          <svg
            width="18"
            height="18"
            viewBox="0 0 24 24"
            fill="none"
            stroke="currentColor"
            strokeWidth="2"
            strokeLinecap="round"
            strokeLinejoin="round"
          >
            <path d="M11 4H4a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2v-7" />
            <path d="M18.5 2.5a2.121 2.121 0 0 1 3 3L12 15l-4 1 1-4 9.5-9.5z" />
          </svg>
          1:1 문의 접수
        </button>
        <button
          type="button"
          className={`cs-tab-btn ${activeTab === "myList" ? "active" : ""}`}
          onClick={() => switchTab("myList")}
        >
          <svg
            width="18"
            height="18"
            viewBox="0 0 24 24"
            fill="none"
            stroke="currentColor"
            strokeWidth="2"
            strokeLinecap="round"
            strokeLinejoin="round"
          >
            <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z" />
            <path d="M14 2v6h6" />
            <path d="M16 13H8" />
            <path d="M16 17H8" />
            <path d="M10 9H8" />
          </svg>
          내 문의 내역
        </button>
      </div>

      {/* MAIN GRID CONTENT */}
      <div className="cs-grid">
        <div className="cs-left">
          {activeTab === "faq" && (
            <FaqPanel searchTerm={searchMode === "faq" ? searchTerm : ""} />
          )}

          {activeTab === "inquire" && (
            <InquiryForm
              onSubmitted={handleInquirySubmitted}
              onCancel={() => switchTab("faq")}
            />
          )}

          {activeTab === "myList" && (
            <MyInquiryList
              refreshKey={myListRefreshKey}
              focusInquiryId={focusInquiryId}
              onNewInquiry={() => switchTab("inquire")}
            />
          )}
        </div>

        {/* RIGHT SIDEBAR */}
        <div className="cs-right">
          <div className="sidebar-card">
            <h3>
              <svg
                width="18"
                height="18"
                viewBox="0 0 24 24"
                fill="none"
                stroke="var(--green-dark)"
                strokeWidth="2"
                strokeLinecap="round"
                strokeLinejoin="round"
              >
                <circle cx="12" cy="12" r="10" />
                <polyline points="12 6 12 12 16 14" />
              </svg>
              고객센터 운영 안내
            </h3>
            <div className="info-list">
              <div className="info-item">
                <span className="label">운영 시간</span>
                <span className="val">평일 09:00 ~ 18:00</span>
              </div>
              <div className="info-item">
                <span className="label">점심 시간</span>
                <span className="val">12:00 ~ 13:00</span>
              </div>
              <div className="info-item">
                <span className="label">휴무일</span>
                <span className="val">주말 및 공휴일</span>
              </div>
              <div className="info-item">
                <span className="label">답변 방법</span>
                <span className="val" style={{ color: "var(--green-dark)" }}>
                  앱 내 알림
                </span>
              </div>
            </div>
          </div>

          <div className="sidebar-card">
            <h3>
              <svg
                width="18"
                height="18"
                viewBox="0 0 24 24"
                fill="none"
                stroke="var(--green-dark)"
                strokeWidth="2"
                strokeLinecap="round"
                strokeLinejoin="round"
              >
                <path d="M18 13v6a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V8a2 2 0 0 1 2-2h6" />
                <polyline points="15 3 21 3 21 9" />
                <line x1="10" y1="14" x2="21" y2="3" />
              </svg>
              자주 찾는 바로가기
            </h3>
            <Link
              to="/dashboard/spending/transactions"
              className="quick-link-btn"
            >
              <span>💳 카드 연동 관리 설정</span>
              <DashboardIcon name="chevron-right" size={15} />
            </Link>
            <Link to="/dashboard/spending" className="quick-link-btn">
              <span>🎯 월급 및 저축 목표 재설정</span>
              <DashboardIcon name="chevron-right" size={15} />
            </Link>
            <Link to="/dashboard/fixed-expenses" className="quick-link-btn">
              <span>🔄 고정지출 등록 및 수정</span>
              <DashboardIcon name="chevron-right" size={15} />
            </Link>
          </div>

          <div
            className="sidebar-card"
            style={{ background: "var(--green-soft)", borderColor: "#bce6ca" }}
          >
            <h3 style={{ color: "var(--green-deep)", marginBottom: "8px" }}>
              💡 서비스 보안 팁
            </h3>
            <p
              style={{
                margin: 0,
                fontSize: "12px",
                color: "#235735",
                lineHeight: "1.5",
              }}
            >
              더모아 지원팀은 어떠한 경우에도 비밀번호나 카드사 원본 비밀번호를
              물어보지 않습니다.
            </p>
          </div>
        </div>
      </div>

      {/* SUCCESS MODAL */}
      <div className={`modal-overlay ${showSuccessModal ? "open" : ""}`}>
        <div className="modal-box">
          <div className="modal-icon">
            <svg
              width="28"
              height="28"
              viewBox="0 0 24 24"
              fill="none"
              stroke="currentColor"
              strokeWidth="2.5"
              strokeLinecap="round"
              strokeLinejoin="round"
            >
              <polyline points="20 6 9 17 4 12" />
            </svg>
          </div>
          <h3>1:1 문의가 접수되었습니다!</h3>
          <p>
            문의해 주신 내용을 확인하여 빠른 시일 내에{" "}
            <strong>앱 내 알림</strong>으로 답변드리겠습니다.
          </p>
          <button
            type="button"
            className="primary-btn"
            onClick={handleCloseModal}
          >
            확인 및 내 문의 보기
          </button>
        </div>
      </div>
    </div>
  );
}

export default CustomerServicePage;
