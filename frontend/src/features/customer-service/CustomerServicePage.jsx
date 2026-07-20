import { useState } from "react";
import { Link } from "react-router-dom";
import DashboardIcon from "../../components/common/DashboardIcon";
import "./CustomerServicePage.css";

const INITIAL_FAQS = [
  {
    id: 1,
    cat: "crd",
    badge: "카드연동",
    title: "카드가 제대로 연동되지 않거나 최근 결제 내역이 안 불러와져요.",
    content: (
      <>
        <p>
          더모아 서비스는 외부 연동(CODEF API)을 통해 매일 새벽 및 앱 진입
          시(30분 간격) 최근 결제 내역을 불러옵니다.
        </p>
        <p>
          <strong>
            아래와 같은 경우 수집이 지연되거나 중단될 수 있습니다:
          </strong>
        </p>
        <ul>
          <li>카드사 비밀번호가 변경되었거나 로그인 세션이 만료된 경우</li>
          <li>
            카드사 비밀번호를 3회 이상 잘못 입력하여 5분간 연결이 일시 제한된
            경우
          </li>
          <li>카드사 서버 점검 시간 (주로 매일 자정 ~ 02시 사이)</li>
        </ul>
        <div className="faq-note">
          <DashboardIcon name="info" size={16} />
          <span>
            <strong>해결 방법:</strong> [카드/소비내역] 또는 [설정] 메뉴에서
            해당 카드사의 <strong>'연결 재시도'</strong> 버튼을 눌러 자격 증명을
            갱신해 보세요.
          </span>
        </div>
      </>
    ),
  },
  {
    id: 2,
    cat: "bud",
    badge: "소비가이드",
    title: "하루 권장 소비액은 어떤 기준으로 계산되나요?",
    content: (
      <>
        <p>
          하루 권장 소비액은 다음 공식으로 계산되며, 당일 하루 동안 고정된
          수치를 유지합니다.
        </p>
        <div
          className="faq-note"
          style={{
            background: "var(--blue-soft)",
            borderColor: "#b8d4ff",
            color: "#1e40af",
          }}
        >
          <span>
            <strong>
              하루 권장 소비액 = (이번 달 월 예산 − 어제까지 누적 사용액) ÷ 이번
              급여주기 남은 날수
            </strong>
          </span>
        </div>
        <p style={{ marginTop: "12px" }}>
          여기서 <strong>월 예산</strong>은{" "}
          <code>월급 − 고정지출 합계 − 월 저축 목표</code> 입니다. 오늘 결제한
          금액이나 취소/환불 내역은 <strong>다음 날 하루 권장액</strong>부터
          반영되므로 오늘 목표가 중간에 불안정하게 흔들리지 않습니다.
        </p>
      </>
    ),
  },
  {
    id: 3,
    cat: "fix",
    badge: "고정지출",
    title:
      "고정지출로 등록해 둔 구독 결제가 하루 지출에서 두 번 빠지는 것 같아요.",
    content: (
      <>
        <p>
          더모아는 고정지출이 월 예산에서 미리 차감되어 하루 권장액에 이중
          계산되는 것을 방지하는 <strong>'이중 차감 방지 및 매칭'</strong>{" "}
          로직을 제공합니다.
        </p>
        <p>
          등록된 고정지출(예: 넷플릭스 17,000원)과 실제 카드 결제 내역이
          매칭되면 당일 소비 지출 합계에서 자동으로 제외 처리됩니다. 만약
          매칭되지 않았다면 가맹점 표기가 달라 일시적으로 미매칭된 것일 수
          있으니, [고정지출 미납 알림]에서 해당 결제건을 선택해 주시면 바로
          정정됩니다.
        </p>
      </>
    ),
  },
  {
    id: 4,
    cat: "exp",
    badge: "수기지출",
    title: "현금이나 계좌이체 지출도 수기로 작성하면 카드 내역과 중복되나요?",
    content: (
      <>
        <p>
          <strong>
            아닙니다! 현금 및 계좌이체 지출은 카드사에 잡히지 않으므로 수기
            입력이 필수적입니다.
          </strong>
        </p>
        <p>
          카드 자동 수집이 켜져 있는 동안에는 동일 건의 중복 방지를 위해 결제
          수단이 '카드'인 지출은 수기 입력을 제한하고 있습니다. 카드를 연동하기
          전에 손으로 작성해둔 과거 카드 수기 지출은 카드 연동 시 자동 정본
          대체(중복 방지)되므로 데이터가 2배로 세어지지 않습니다.
        </p>
      </>
    ),
  },
  {
    id: 5,
    cat: "sec",
    badge: "계정&보안",
    title: "비밀번호를 바꾸거나 회원 탈퇴를 하면 기존 정보는 어떻게 되나요?",
    content: (
      <>
        <p>
          <strong>1) 비밀번호 변경 시:</strong> 계정 보안을 위하여 비밀번호 변경
          즉시 접속 중이던 <strong>모든 기기에서 즉시 강제 로그아웃</strong>{" "}
          처리됩니다.
        </p>
        <p>
          <strong>2) 회원 탈퇴 시:</strong> 등록해 두신 카드사 연결 열쇠(Auth
          Token)가 즉시 파기되며, 새벽 자동 수집 대상에서 즉시 제외됩니다.
          수집되었던 금융 데이터는 관계 법령에 따라 안전하게 즉시 삭제 또는
          파기됩니다.
        </p>
      </>
    ),
  },
];

const INITIAL_INQUIRIES = [
  {
    id: 1,
    category: "카드 연동 / 수집 오류",
    title: "현대카드 자동 수집이 30분 넘게 안 들어와요.",
    content:
      "현대카드를 새로 연결했는데 최근 결제한 2건이 계속 안 뜨고 새로고침을 눌러도 반응이 없습니다.",
    status: "done",
    statusText: "답변 완료",
    date: "2026-07-19 14:22",
    response:
      "안녕하세요, 회원님! 더모아 고객센터입니다.\n확인 결과 현대카드 측 가맹점 승인 시스템 일시 점검으로 인해 약 40분간 결제 데이터 연동 수집이 지연되었습니다.\n현재 정상 복구되었으며 [카드/소비내역] 메뉴에서 '새로고침'을 누르시면 정상 반영됩니다. 이용에 불편을 드려 죄송합니다.",
    responseDate: "2026-07-19 16:05",
  },
  {
    id: 2,
    category: "고정지출 / 미납 알림",
    title: "해외 구독(Claude) 환율 계산이 달라요.",
    content:
      "달러 결제($22) 구독인데 실제 청구 원화 금액과 차이가 나서 미납 알림이 뜨는 것 같습니다.",
    status: "pending",
    statusText: "답변 대기중",
    date: "2026-07-20 11:05",
    response: null,
    responseDate: null,
  },
];

function CustomerServicePage() {
  const [activeTab, setActiveTab] = useState("faq");
  const [selectedCat, setSelectedCat] = useState("all");
  const [searchTerm, setSearchTerm] = useState("");
  const [openFaqId, setOpenFaqId] = useState(1);
  const [feedbackGiven, setFeedbackGiven] = useState({});
  const [inquiries, setInquiries] = useState(INITIAL_INQUIRIES);

  // Form State
  const [inquiryType, setInquiryType] = useState("");
  const [inquiryEmail, setInquiryEmail] = useState("solmin@example.com");
  const [inquiryTitle, setInquiryTitle] = useState("");
  const [inquiryContent, setInquiryContent] = useState("");
  const [selectedFile, setSelectedFile] = useState(null);
  const [agreeTerms, setAgreeTerms] = useState(false);

  // Success Modal State
  const [showSuccessModal, setShowSuccessModal] = useState(false);

  const handleSearchTag = (tag) => {
    setSearchTerm(tag);
    setActiveTab("faq");
  };

  const toggleFaq = (id) => {
    setOpenFaqId((prev) => (prev === id ? null : id));
  };

  const handleFeedback = (faqId) => {
    setFeedbackGiven((prev) => ({ ...prev, [faqId]: true }));
  };

  const handleFileChange = (e) => {
    if (e.target.files && e.target.files[0]) {
      setSelectedFile(e.target.files[0]);
    } else {
      setSelectedFile(null);
    }
  };

  const handleInquirySubmit = (e) => {
    e.preventDefault();
    if (!inquiryType || !inquiryTitle || !inquiryContent || !agreeTerms) {
      return;
    }

    const now = new Date();
    const dateStr =
      now.getFullYear() +
      "-" +
      String(now.getMonth() + 1).padStart(2, "0") +
      "-" +
      String(now.getDate()).padStart(2, "0") +
      " " +
      String(now.getHours()).padStart(2, "0") +
      ":" +
      String(now.getMinutes()).padStart(2, "0");

    const newInquiry = {
      id: Date.now(),
      category: inquiryType,
      title: `[${inquiryType}] ${inquiryTitle}`,
      content: inquiryContent,
      status: "pending",
      statusText: "답변 대기중",
      date: dateStr,
      response: null,
      responseDate: null,
    };

    setInquiries((prev) => [newInquiry, ...prev]);
    setShowSuccessModal(true);
  };

  const handleCloseModal = () => {
    setShowSuccessModal(false);
    setInquiryType("");
    setInquiryTitle("");
    setInquiryContent("");
    setSelectedFile(null);
    setAgreeTerms(false);
    setActiveTab("myList");
  };

  const filteredFaqs = INITIAL_FAQS.filter((faq) => {
    const matchesCat = selectedCat === "all" || faq.cat === selectedCat;
    const searchLower = searchTerm.toLowerCase().trim();
    if (!searchLower) return matchesCat;

    const matchesSearch =
      faq.title.toLowerCase().includes(searchLower) ||
      faq.badge.toLowerCase().includes(searchLower);
    return matchesCat && matchesSearch;
  });

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
            자주 묻는 질문(FAQ)에서 빠르게 해결하거나, 해결되지 않은 문제는 1:1
            문의를 남겨주시면 친절하게 답변드리겠습니다.
          </p>

          {/* SEARCH BOX */}
          <div className="cs-search-box">
            <DashboardIcon name="search" size={18} />
            <input
              type="text"
              placeholder="궁금한 답변이나 키워드를 검색해보세요 (예: 카드연동, 하루권장액, 수기지출)"
              value={searchTerm}
              onChange={(e) => setSearchTerm(e.target.value)}
            />
            <button
              type="button"
              className="cs-search-btn"
              onClick={() => setActiveTab("faq")}
            >
              검색하기
            </button>
          </div>

          {/* QUICK TAGS */}
          <div className="quick-tags">
            <span>인기 키워드:</span>
            <button
              type="button"
              className="tag-chip"
              onClick={() => handleSearchTag("카드연동")}
            >
              #카드연동
            </button>
            <button
              type="button"
              className="tag-chip"
              onClick={() => handleSearchTag("하루권장액")}
            >
              #하루권장액
            </button>
            <button
              type="button"
              className="tag-chip"
              onClick={() => handleSearchTag("고정지출")}
            >
              #고정지출
            </button>
            <button
              type="button"
              className="tag-chip"
              onClick={() => handleSearchTag("수기지출")}
            >
              #수기지출
            </button>
            <button
              type="button"
              className="tag-chip"
              onClick={() => handleSearchTag("비밀번호")}
            >
              #비밀번호
            </button>
          </div>
        </div>
      </section>

      {/* TABS BAR */}
      <div className="cs-tabs-bar">
        <button
          type="button"
          className={`cs-tab-btn ${activeTab === "faq" ? "active" : ""}`}
          onClick={() => setActiveTab("faq")}
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
          onClick={() => setActiveTab("inquire")}
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
          onClick={() => setActiveTab("myList")}
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
          <span className="cs-tab-badge">{inquiries.length}</span>
        </button>
      </div>

      {/* MAIN GRID CONTENT */}
      <div className="cs-grid">
        {/* LEFT SECTION (TAB VIEWS) */}
        <div className="cs-left">
          {/* TAB 1: FAQ VIEW */}
          {activeTab === "faq" && (
            <div>
              {/* CATEGORY FILTERS */}
              <div className="faq-categories">
                <button
                  type="button"
                  className={`cat-btn ${selectedCat === "all" ? "active" : ""}`}
                  onClick={() => setSelectedCat("all")}
                >
                  전체보기
                </button>
                <button
                  type="button"
                  className={`cat-btn ${selectedCat === "crd" ? "active" : ""}`}
                  onClick={() => setSelectedCat("crd")}
                >
                  💳 카드 연동/수집
                </button>
                <button
                  type="button"
                  className={`cat-btn ${selectedCat === "bud" ? "active" : ""}`}
                  onClick={() => setSelectedCat("bud")}
                >
                  🎯 소비 가이드
                </button>
                <button
                  type="button"
                  className={`cat-btn ${selectedCat === "fix" ? "active" : ""}`}
                  onClick={() => setSelectedCat("fix")}
                >
                  🔄 고정지출/구독
                </button>
                <button
                  type="button"
                  className={`cat-btn ${selectedCat === "exp" ? "active" : ""}`}
                  onClick={() => setSelectedCat("exp")}
                >
                  ✏️ 수기 지출
                </button>
                <button
                  type="button"
                  className={`cat-btn ${selectedCat === "sec" ? "active" : ""}`}
                  onClick={() => setSelectedCat("sec")}
                >
                  🔒 계정 & 보안
                </button>
              </div>

              {/* ACCORDION FAQ ITEMS */}
              <div className="faq-list">
                {filteredFaqs.length > 0 ? (
                  filteredFaqs.map((item) => {
                    const isOpen = openFaqId === item.id;
                    return (
                      <div
                        key={item.id}
                        className={`faq-item ${isOpen ? "open" : ""}`}
                      >
                        <button
                          type="button"
                          className="faq-question"
                          onClick={() => toggleFaq(item.id)}
                        >
                          <span className="q-badge">{item.badge}</span>
                          <span className="q-title">{item.title}</span>
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
                            {item.content}
                            <div className="faq-feedback">
                              <span>이 답변이 도움이 되었나요?</span>
                              <div className="feedback-btns">
                                {feedbackGiven[item.id] ? (
                                  <span
                                    style={{
                                      color: "var(--green-dark)",
                                      fontWeight: 700,
                                    }}
                                  >
                                    소중한 의견 감사합니다!
                                  </span>
                                ) : (
                                  <>
                                    <button
                                      type="button"
                                      className="feedback-btn"
                                      onClick={() => handleFeedback(item.id)}
                                    >
                                      👍 예
                                    </button>
                                    <button
                                      type="button"
                                      className="feedback-btn"
                                      onClick={() => handleFeedback(item.id)}
                                    >
                                      👎 아니오
                                    </button>
                                  </>
                                )}
                              </div>
                            </div>
                          </div>
                        </div>
                      </div>
                    );
                  })
                ) : (
                  <div className="faq-empty">
                    검색 결과와 일치하는 FAQ 질문이 없습니다.
                  </div>
                )}
              </div>
            </div>
          )}

          {/* TAB 2: INQUIRY FORM VIEW */}
          {activeTab === "inquire" && (
            <div className="panel">
              <h2 style={{ margin: "0 0 8px", fontSize: "18px" }}>
                1:1 문의 작성하기
              </h2>
              <p
                style={{
                  margin: "0 0 24px",
                  color: "var(--text-2)",
                  fontSize: "14px",
                }}
              >
                궁금한 점이나 서비스 오류를 남겨주시면 확인 후 이메일과 앱 내
                알림으로 답변해 드립니다.
              </p>

              <form onSubmit={handleInquirySubmit}>
                <div className="form-group">
                  <label className="form-label">
                    문의 유형 <span className="req">*</span>
                  </label>
                  <select
                    className="form-control"
                    value={inquiryType}
                    onChange={(e) => setInquiryType(e.target.value)}
                    required
                  >
                    <option value="">문의 유형을 선택해 주세요</option>
                    <option value="카드 연동 / 수집 오류">
                      💳 카드 연동 / 수집 오류
                    </option>
                    <option value="소비 가이드 / 하루 권장액">
                      🎯 소비 가이드 / 하루 권장액
                    </option>
                    <option value="고정지출 / 미납 알림">
                      🔄 고정지출 / 미납 알림
                    </option>
                    <option value="수기 지출 / 입력 모드">
                      ✏️ 수기 지출 / 입력 모드
                    </option>
                    <option value="계정 / 로그인 / 비밀번호">
                      🔒 계정 / 로그인 / 비밀번호
                    </option>
                    <option value="기타 문의 및 서비스 건의">
                      💡 기타 문의 및 서비스 건의
                    </option>
                  </select>
                </div>

                <div className="form-group">
                  <label className="form-label">
                    답변 받으실 이메일 <span className="req">*</span>
                  </label>
                  <input
                    type="email"
                    className="form-control"
                    value={inquiryEmail}
                    onChange={(e) => setInquiryEmail(e.target.value)}
                    placeholder="example@email.com"
                    required
                  />
                </div>

                <div className="form-group">
                  <label className="form-label">
                    문의 제목 <span className="req">*</span>
                  </label>
                  <input
                    type="text"
                    className="form-control"
                    value={inquiryTitle}
                    onChange={(e) => setInquiryTitle(e.target.value)}
                    placeholder="제목을 입력해 주세요 (예: 카드 연동 중 오류 코드가 뜹니다)"
                    required
                  />
                </div>

                <div className="form-group">
                  <label className="form-label">
                    문의 내용 <span className="req">*</span>
                  </label>
                  <textarea
                    className="form-control"
                    value={inquiryContent}
                    onChange={(e) => setInquiryContent(e.target.value)}
                    placeholder="자세한 문의 내용이나 발생 상황을 작성해 주시면 더욱 정확하고 빠른 답변이 가능합니다."
                    required
                  />
                </div>

                <div className="form-group">
                  <label className="form-label">
                    화면 캡처 파일 첨부 (선택)
                  </label>
                  <label htmlFor="fileInput" className="file-dropzone">
                    <div className="icon-box">
                      <svg
                        width="20"
                        height="20"
                        viewBox="0 0 24 24"
                        fill="none"
                        stroke="currentColor"
                        strokeWidth="2"
                        strokeLinecap="round"
                        strokeLinejoin="round"
                      >
                        <path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4" />
                        <polyline points="17 8 12 3 7 8" />
                        <line x1="12" y1="3" x2="12" y2="15" />
                      </svg>
                    </div>
                    <p>
                      이곳을 클릭하거나 파일 이미지(PNG, JPG)를 끌어다 놓으세요
                    </p>
                    <span>최대 10MB 이하의 이미지 파일 3개까지 첨부 가능</span>
                    <input
                      type="file"
                      id="fileInput"
                      hidden
                      accept="image/*"
                      onChange={handleFileChange}
                    />
                  </label>
                  {selectedFile && (
                    <div
                      style={{
                        marginTop: "10px",
                        fontSize: "12px",
                        color: "var(--green-dark)",
                        fontWeight: "650",
                      }}
                    >
                      📎 선택된 파일: {selectedFile.name} (
                      {(selectedFile.size / 1024).toFixed(1)} KB)
                    </div>
                  )}
                </div>

                <div className="form-group">
                  <label className="checkbox-label">
                    <input
                      type="checkbox"
                      checked={agreeTerms}
                      onChange={(e) => setAgreeTerms(e.target.checked)}
                      required
                    />
                    <span>
                      문의 처리 및 답변 안내를 위한{" "}
                      <strong>개인정보 수집 및 이용</strong>에 동의합니다.
                    </span>
                  </label>
                </div>

                <div className="form-actions">
                  <button
                    type="button"
                    className="secondary-btn"
                    onClick={() => setActiveTab("faq")}
                  >
                    취소
                  </button>
                  <button type="submit" className="primary-btn">
                    문의 등록하기
                  </button>
                </div>
              </form>
            </div>
          )}

          {/* TAB 3: MY INQUIRIES VIEW */}
          {activeTab === "myList" && (
            <div>
              <div
                style={{
                  display: "flex",
                  justifyContent: "space-between",
                  alignItems: "center",
                  marginBottom: "18px",
                }}
              >
                <h2 style={{ margin: 0, fontSize: "18px" }}>
                  내가 작성한 문의 목록
                </h2>
                <button
                  type="button"
                  className="secondary-btn"
                  style={{ minHeight: "36px", fontSize: "13px" }}
                  onClick={() => setActiveTab("inquire")}
                >
                  + 새 문의 작성
                </button>
              </div>

              <div>
                {inquiries.map((item) => (
                  <div key={item.id} className="inquiry-card">
                    <div className="inquiry-header">
                      <span
                        className={`inquiry-status ${
                          item.status === "done" ? "done" : "pending"
                        }`}
                      >
                        {item.statusText}
                      </span>
                      <span className="inquiry-date">{item.date}</span>
                    </div>
                    <h3 className="inquiry-title">{item.title}</h3>
                    <p
                      style={{
                        margin: "0 0 10px",
                        fontSize: "13px",
                        color: "var(--text-2)",
                      }}
                    >
                      {item.content}
                    </p>

                    {item.response && (
                      <div className="inquiry-response">
                        <strong>
                          💬 더모아 고객센터 답변 ({item.responseDate})
                        </strong>
                        {item.response.split("\n").map((line, idx) => (
                          <span key={idx}>
                            {line}
                            <br />
                          </span>
                        ))}
                      </div>
                    )}
                  </div>
                ))}
              </div>
            </div>
          )}
        </div>

        {/* RIGHT SIDEBAR */}
        <div className="cs-right">
          {/* CUSTOMER SERVICE INFO CARD */}
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
                <span className="label">평균 답변 시간</span>
                <span className="val" style={{ color: "var(--green-dark)" }}>
                  24시간 이내
                </span>
              </div>
              <div
                className="info-item"
                style={{
                  paddingTop: "8px",
                  borderTop: "1px dashed var(--border)",
                }}
              >
                <span className="label">고객지원 이메일</span>
                <span className="val">support@themoa.co.kr</span>
              </div>
            </div>
          </div>

          {/* QUICK HELPFUL LINKS CARD */}
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

          {/* NOTICE BANNER */}
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
            <strong>등록하신 이메일과 앱 내 알림</strong>으로 답변드리겠습니다.
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
