import { Routes, Route } from "react-router-dom";
import LandingPage from "../features/landing/LandingPage";
import LoginPage from "../features/auth/LoginPage";
import SignupPage from "../features/auth/SignupPage";
import Dashboard from "../features/dashboard/Dashboard";
import ProductsPage from "../features/products/ProductsPage";
import PolicyPage from "../features/policy/PolicyPage";
import PolicyAdminPage from "../features/policy/admin/PolicyAdminPage";
import SpendingGuidePage from "../features/spending-guide/SpendingGuidePage";
import SpendingHistoryPage from "../features/spending-guide/SpendingHistoryPage";
import CategoryDetailPage from "../features/spending-guide/CategoryDetailPage";
import FixedExpensePage from "../features/fixed-expense/FixedExpensePage";
import MyPage from "../features/mypage/MyPage";
import CustomerServicePage from "../features/customer-service/CustomerServicePage";
import CustomerServiceAdminPage from "../features/customer-service-admin/CustomerServiceAdminPage";
import ProtectedRoute from "./ProtectedRoute";
import AdminRoute from "./AdminRoute";
import UserOnlyRoute from "./UserOnlyRoute";
import DashboardLayout from "../components/layout/DashboardLayout";

const policyLocalToolsEnabled =
  import.meta.env.DEV &&
  import.meta.env.VITE_POLICY_LOCAL_TOOLS_ENABLED === "true";

function AppRouter() {
  return (
    <Routes>
      <Route path="/" element={<LandingPage />} />
      <Route path="/login" element={<LoginPage />} />
      <Route path="/signup" element={<SignupPage />} />

      {policyLocalToolsEnabled && (
        <Route path="/dashboard/policy/admin" element={<PolicyAdminPage />} />
      )}

      <Route element={<ProtectedRoute />}>
        <Route element={<UserOnlyRoute />}>
          <Route path="/dashboard" element={<DashboardLayout />}>
            <Route index element={<Dashboard />} />
            <Route path="products" element={<ProductsPage />} />
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
        </Route>
      </Route>
    </Routes>
  );
}

export default AppRouter;
