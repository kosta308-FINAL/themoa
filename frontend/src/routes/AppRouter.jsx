import { Routes, Route } from "react-router-dom";
import LandingPage from "../features/landing/LandingPage";
import LoginPage from "../features/auth/LoginPage";
import SignupPage from "../features/auth/SignupPage";
import Dashboard from "../features/dashboard/Dashboard";
import ProductsPage from "../features/products/ProductsPage";
import FinancialSearchPage from "../features/financial-search/FinancialSearchPage";
import PolicyPage from "../features/policy/PolicyPage";
import PolicyAdminPage from "../features/policy/admin/PolicyAdminPage";
import SpendingGuidePage from "../features/spending-guide/SpendingGuidePage";
import SpendingHistoryPage from "../features/spending-guide/SpendingHistoryPage";
import CategoryDetailPage from "../features/spending-guide/CategoryDetailPage";
import FixedExpensePage from "../features/fixed-expense/FixedExpensePage";
import MyPage from "../features/mypage/MyPage";
import CustomerServicePage from "../features/customer-service/CustomerServicePage";
import CustomerServiceAdminPage from "../features/customer-service-admin/CustomerServiceAdminPage";
import CustomerServiceAiQualityPage from "../features/customer-service-admin/CustomerServiceAiQualityPage";
import MerchantMasterAdminPage from "../features/merchant-admin/MerchantMasterAdminPage";
import FinancialProductAdminPage from "../features/financial-admin/FinancialProductAdminPage";
import ProtectedRoute from "./ProtectedRoute";
import AdminRoute from "./AdminRoute";
import UserOnlyRoute from "./UserOnlyRoute";
import DashboardLayout from "../components/layout/DashboardLayout";

function AppRouter() {
  return (
    <Routes>
      <Route path="/" element={<LandingPage />} />
      <Route path="/login" element={<LoginPage />} />
      <Route path="/signup" element={<SignupPage />} />

      <Route element={<ProtectedRoute />}>
        <Route element={<UserOnlyRoute />}>
          <Route path="/dashboard" element={<DashboardLayout />}>
            <Route index element={<Dashboard />} />
            <Route path="products" element={<ProductsPage />} />
            <Route path="products/search" element={<FinancialSearchPage />} />
            <Route path="policy" element={<PolicyPage />} />
            <Route path="spending" element={<SpendingGuidePage />} />
            <Route
              path="spending/transactions"
              element={<SpendingHistoryPage />}
            />
            <Route
              path="spending/category-detail"
              element={<CategoryDetailPage />}
            />
            <Route path="fixed-expenses" element={<FixedExpensePage />} />
            <Route path="mypage" element={<MyPage />} />
            <Route
              path="customer-service"
              element={<CustomerServicePage />}
            />
          </Route>
        </Route>

        <Route element={<AdminRoute />}>
          <Route
            path="/admin/customer-service"
            element={<CustomerServiceAdminPage />}
          />
          <Route
            path="/admin/customer-service/ai-quality"
            element={<CustomerServiceAiQualityPage />}
          />
          <Route path="/admin/merchants" element={<MerchantMasterAdminPage />} />
          <Route path="/admin/policies" element={<PolicyAdminPage />} />
          <Route
            path="/admin/financial-products"
            element={<FinancialProductAdminPage />}
          />
        </Route>
      </Route>
    </Routes>
  );
}

export default AppRouter;
