import { Routes, Route } from 'react-router-dom'
import LandingPage from '../features/landing/LandingPage'
import LoginPage from '../features/auth/LoginPage'
import SignupPage from '../features/auth/SignupPage'
import Dashboard from '../features/dashboard/Dashboard'
import ProductsPage from '../features/products/ProductsPage'
import PolicyPage from '../features/policy/PolicyPage'
import SpendingGuidePage from '../features/spending-guide/SpendingGuidePage'
import SpendingHistoryPage from '../features/spending-guide/SpendingHistoryPage'
import MyPage from '../features/mypage/MyPage'
import SectionStub from '../components/common/SectionStub'

function AppRouter() {
  return (
    <Routes>
      <Route path="/" element={<LandingPage />} />
      <Route path="/login" element={<LoginPage />} />
      <Route path="/signup" element={<SignupPage />} />
      <Route path="/dashboard" element={<Dashboard />} />
      <Route path="/dashboard/products" element={<ProductsPage />} />
      <Route path="/dashboard/policy" element={<PolicyPage />} />
      <Route path="/dashboard/spending" element={<SpendingGuidePage />} />
      <Route path="/dashboard/spending/transactions" element={<SpendingHistoryPage />} />
      <Route path="/dashboard/fixed-expenses" element={<SectionStub title="고정지출" description="매달 반복되는 지출을 관리합니다." />} />
      <Route path="/dashboard/mypage" element={<MyPage />} />
    </Routes>
  )
}

export default AppRouter
