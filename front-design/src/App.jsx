import { useState } from 'react'
import DashboardHeader from './components/DashboardHeader'
import DashboardPage from './components/DashboardPage'
import EmptyState from './components/EmptyState'
import './App.css'

const TABS = [
  { id: 'dashboard', label: '대시보드' },
  { id: 'goals', label: '목표' },
  { id: 'expenses', label: '반복지출' },
  { id: 'policies', label: '정책' },
  { id: 'products', label: '금융상품' },
]

const PLACEHOLDER_COPY = {
  goals: {
    title: '아직 등록된 목표가 없어요',
    desc: '목표를 추가하면 달성률과 남은 기간을 한눈에 확인할 수 있어요',
  },
  expenses: {
    title: '등록된 반복지출이 없어요',
    desc: '구독·고정비를 등록하면 결제일과 총 지출을 관리할 수 있어요',
  },
  policies: {
    title: '데이터를 입력해주세요',
    desc: '거주지, 나이 등 기본 정보를 입력하면 맞춤 정책을 추천해드려요',
  },
  products: {
    title: '데이터를 입력해주세요',
    desc: '투자 성향을 입력하면 맞춤 금융상품을 추천해드려요',
  },
}

function App() {
  const [activeTab, setActiveTab] = useState('dashboard')

  return (
    <div className="dashboard-page-wrapper">
      <DashboardHeader tabs={TABS} activeTab={activeTab} onChange={setActiveTab} />
      <main className="dashboard-main">
        {activeTab === 'dashboard' ? (
          <DashboardPage onNavigate={setActiveTab} />
        ) : (
          <>
            <div className="dash-heading">
              <h2>{TABS.find((t) => t.id === activeTab)?.label}</h2>
            </div>
            <div className="placeholder-card">
              <EmptyState title={PLACEHOLDER_COPY[activeTab].title} description={PLACEHOLDER_COPY[activeTab].desc} />
            </div>
          </>
        )}
      </main>
    </div>
  )
}

export default App
