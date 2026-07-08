import { Routes, Route } from 'react-router-dom'
import LandingPage from '../pages/LandingPage/LandingPage'
import Dashboard from '../pages/Dashboard/Dashboard'
import ProductsPage from '../pages/dashboard-sections/ProductsPage'
import PolicyPage from '../pages/dashboard-sections/PolicyPage'
import SpendingGuidePage from '../pages/dashboard-sections/SpendingGuidePage'
import LoanComparePage from '../pages/dashboard-sections/LoanComparePage'
import MyPage from '../pages/dashboard-sections/MyPage'

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
