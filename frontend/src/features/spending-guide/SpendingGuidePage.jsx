import { useState } from 'react'
import DashboardIcon from '../../components/common/DashboardIcon'
import DashboardTopNav from '../../components/layout/DashboardTopNav'
import DashboardFooter from '../../components/layout/DashboardFooter'
import '../dashboard/Dashboard.css'
import './SpendingGuidePage.css'

function EmptyState({ icon, title, description }) {
  return (
    <div className="spending-empty">
      <span className="spending-empty-icon"><DashboardIcon name={icon} size={22} /></span>
      <strong>{title}</strong>
      <p>{description}</p>
    </div>
  )
}

function PanelTitle({ icon, title, description, tone = 'green' }) {
  return (
    <div className="spending-panel-title">
      <span className={`spending-panel-icon ${tone}`}><DashboardIcon name={icon} size={18} /></span>
      <div>
        <h2>{title}</h2>
        <p>{description}</p>
      </div>
    </div>
  )
}

function ManualEntryModal({ onClose }) {
  const handleSubmit = (event) => event.preventDefault()

  return (
    <div className="spending-modal-backdrop" role="presentation" onMouseDown={onClose}>
      <section
        className="spending-modal"
        role="dialog"
        aria-modal="true"
        aria-labelledby="manual-entry-title"
        onMouseDown={(event) => event.stopPropagation()}
      >
        <div className="spending-modal-head">
          <div>
            <h2 id="manual-entry-title">지출 직접 입력</h2>
            <p>현금과 계좌이체 내역을 기록하는 화면입니다.</p>
          </div>
          <button type="button" className="spending-modal-close" onClick={onClose} aria-label="닫기">×</button>
        </div>
        <form className="spending-entry-form" onSubmit={handleSubmit}>
          <label>
            <span>금액 *</span>
            <div className="spending-input-suffix"><input inputMode="numeric" placeholder="0" /><em>원</em></div>
          </label>
          <label>
            <span>결제수단 *</span>
            <select defaultValue=""><option value="" disabled>선택</option><option>현금</option><option>계좌이체</option></select>
          </label>
          <label className="wide">
            <span>사용처/내용 *</span>
            <input placeholder="사용처나 지출 내용을 입력하세요" />
          </label>
          <label>
            <span>사용일시 *</span>
            <input type="datetime-local" />
          </label>
          <label>
            <span>카테고리 *</span>
            <select defaultValue=""><option value="" disabled>선택</option><option>식비</option><option>카페/간식</option><option>편의점</option><option>교통</option><option>쇼핑</option><option>문화/여가</option><option>생활</option><option>의료</option><option>교육</option><option>여행</option><option>경조사</option><option>기타</option></select>
          </label>
          <label className="wide">
            <span>메모</span>
            <textarea placeholder="기억해둘 내용을 적어주세요" />
          </label>
          <div className="spending-form-notice wide">
            <DashboardIcon name="info" size={17} />
            <span>현재는 화면만 구성되어 있어 저장 기능은 연결되지 않았습니다.</span>
          </div>
          <button type="submit" className="spending-primary wide" disabled>지출 기록하기</button>
        </form>
      </section>
    </div>
  )
}

function SpendingGuidePage() {
  const [isEntryOpen, setIsEntryOpen] = useState(false)

  return (
    <div className="dashboard spending-guide">
      <DashboardTopNav />
      <main className="spending-main">
        <header className="spending-page-head">
          <div>
            <h1>소비가이드</h1>
            <p>오늘의 기준을 확인하고, 무리 없이 쓸 수 있는 금액을 관리해보세요.</p>
          </div>
        </header>

        <section className="spending-hero" aria-label="소비 기준 요약">
          <article className="spending-today-card">
            <div className="spending-card-head">
              <div><h2>오늘의 소비 기준</h2><p>소비 정보가 연결되면 오늘의 권장 금액을 보여드려요.</p></div>
              <span className="spending-status">정보 연결 전</span>
            </div>
            <div className="spending-number-grid">
              <div className="spending-number-main">
                <span><DashboardIcon name="wallet" size={16} />오늘 사용 가능 금액</span>
                <strong>—</strong>
                <p>데이터 연결 후 계산됩니다.</p>
              </div>
              <div className="spending-mini-stat"><span>하루 권장 소비액</span><strong>—</strong><p>데이터 연결 전</p></div>
              <div className="spending-mini-stat"><span>오늘 순사용액</span><strong>—</strong><p>데이터 연결 전</p></div>
            </div>
            <div className="spending-progress-meta"><span>오늘 권장액 사용률</span><strong>—</strong></div>
            <div className="spending-progress" aria-hidden="true" />
          </article>

          <aside className="spending-cycle-card">
            <span>이번 급여 주기</span>
            <h2>남은 예산</h2>
            <strong className="spending-cycle-amount">—</strong>
            <p>급여 주기 정보가 없습니다.</p>
            <div className="spending-cycle-bottom">
              <div><span>남은 기간</span><strong>—</strong></div>
              <div><span>주기 순사용액</span><strong>—</strong></div>
            </div>
          </aside>
        </section>

        <div className="spending-content-grid">
          <div className="spending-column">
            <section className="spending-panel">
              <div className="spending-panel-head">
                <PanelTitle icon="receipt" title="오늘 거래" description="고정지출을 제외한 오늘의 거래를 보여드려요" />
                <button type="button" className="spending-secondary" onClick={() => setIsEntryOpen(true)}><DashboardIcon name="plus" size={15} />지출 직접 입력</button>
              </div>
              <EmptyState icon="receipt" title="아직 표시할 소비내역이 없어요" description="거래 데이터가 연결되면 오늘의 소비내역이 여기에 표시됩니다." />
              <div className="spending-panel-footer"><button type="button" disabled>전체 소비내역 보기</button></div>
            </section>

            <section className="spending-panel spending-flow-panel">
              <div className="spending-panel-head">
                <PanelTitle icon="chart" title="최근 7일 소비 흐름" description="날짜별 순사용액과 하루 권장액을 비교해요" tone="blue" />
              </div>
              <EmptyState icon="chart" title="소비 흐름을 만들 데이터가 없어요" description="거래 데이터가 쌓이면 최근 7일의 소비 흐름을 보여드려요." />
            </section>
          </div>

          <div className="spending-column">
            <section className="spending-panel spending-category-panel">
              <div className="spending-panel-head">
                <PanelTitle icon="chart" title="카테고리별 소비" description="실제 소비 순액을 기준으로 보여드려요" tone="teal" />
              </div>
              <EmptyState icon="chart" title="분석할 카테고리 데이터가 없어요" description="소비내역이 연결되면 카테고리별 비중이 표시됩니다." />
              <div className="spending-panel-footer"><button type="button" disabled>카테고리 상세보기</button></div>
            </section>

            <section className="spending-panel">
              <div className="spending-panel-head">
                <PanelTitle icon="repeat" title="고정지출 후보" description="반복되는 결제를 찾아 알려드려요" tone="orange" />
              </div>
              <EmptyState icon="repeat" title="아직 발견된 고정지출 후보가 없어요" description="거래가 쌓이면 반복 결제 패턴을 이곳에서 확인할 수 있어요." />
            </section>

            <section className="spending-panel">
              <div className="spending-panel-head">
                <PanelTitle icon="sparkle" title="이번 달 이렇게 아껴봐요" description="지난 소비 습관을 바탕으로 알려드려요" tone="purple" />
              </div>
              <EmptyState icon="sparkle" title="아직 제공할 소비 코칭이 없어요" description="분석 가능한 소비내역이 쌓이면 맞춤 코칭을 보여드려요." />
            </section>
          </div>
        </div>
      </main>
      <DashboardFooter />
      {isEntryOpen && <ManualEntryModal onClose={() => setIsEntryOpen(false)} />}
    </div>
  )
}

export default SpendingGuidePage
