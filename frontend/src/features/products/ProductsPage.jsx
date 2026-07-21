import { Link } from "react-router-dom";
import Toast from "../../components/common/Toast";
import { useBookmarks } from "../../hooks/useBookmarks";
import RecommendForm from "./components/RecommendForm";
import RecommendResults from "./components/RecommendResults";
import { useRecommend } from "./hooks/useRecommend";
import { useRecommendDefaults } from "./hooks/useRecommendDefaults";
import "./ProductsPage.css";

function ProductsPage() {
  const { data, loading, error, searched, runRecommend } = useRecommend();
  const { defaults, loading: defaultsLoading } = useRecommendDefaults();
  const bookmarks = useBookmarks();

  return (
    <main className="dash-main products-page">
      <div className="dash-topbar">
        <div>
          <h1>맞춤형 금융상품 추천</h1>
          <p>
            내 정보를 입력하면 가입 가능한 예·적금 중 딱 맞는 상품을 골라드려요.
          </p>
        </div>
        <Link className="products-nav-link" to="/dashboard/products/search">
          상품 검색하기 →
        </Link>
      </div>

      <div className="products-layout">
        {/* 기본값이 준비된 뒤에 폼을 올려서, 서버 값을 초기값으로 그대로 쓰게 한다. */}
        {defaultsLoading ? (
          <div className="rec-form rec-form-loading">
            내 정보를 불러오고 있어요…
          </div>
        ) : (
          <RecommendForm
            loading={loading}
            defaults={defaults}
            onSubmit={runRecommend}
          />
        )}
        <section className="products-results-col">
          <RecommendResults
            data={data}
            loading={loading}
            error={error}
            searched={searched}
            bookmarks={bookmarks}
          />
        </section>
      </div>

      <Toast toast={bookmarks.toast} onClose={bookmarks.clearToast} />
    </main>
  );
}

export default ProductsPage;
