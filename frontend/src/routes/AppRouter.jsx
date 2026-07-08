import { Routes, Route } from 'react-router-dom'
import LandingPage from '../features/landing/LandingPage'
import Dashboard from '../features/dashboard/Dashboard'
import ProductsPage from '../features/products/ProductsPage'
import PolicyPage from '../features/policy/PolicyPage'
import SpendingGuidePage from '../features/spending-guide/SpendingGuidePage'
import LoanComparePage from '../features/loan-compare/LoanComparePage'
import MyPage from '../features/mypage/MyPage'

function AppRouter() {
  return (
    <Routes>
      <Route path="/" element={<LandingPage />} />
      <Route path="/dashboard" element={<Dashboard />} />
      <Route path="/dashboard/products" element={<ProductsPage />} />
      <Route path="/dashboard/policy" element={<PolicyPage />} />
      <Route path="/dashboard/spending" element={<SpendingGuidePage />} />
      <Route path="/dashboard/loan" element={<LoanComparePage />} />
      <Route path="/dashboard/mypage" element={<MyPage />} />
    </Routes>
  )
}

export default AppRouter
