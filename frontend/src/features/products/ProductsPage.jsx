import { Link, useNavigate } from "react-router-dom";
import Toast from "../../components/common/Toast";
import { useBookmarks } from "../../hooks/useBookmarks";
import RecommendForm from "./components/RecommendForm";
import RecommendResults from "./components/RecommendResults";
import MaturityChart from "./components/MaturityChart";
import PopularProducts from "./components/PopularProducts";
import { useRecommend } from "./hooks/useRecommend";
import { useRecommendDefaults } from "./hooks/useRecommendDefaults";
import { usePopularProducts } from "./hooks/usePopularProducts";
import "./ProductsPage.css";

function ProductsPage() {
  const navigate = useNavigate();
  const { data, loading, error, searched, runRecommend } = useRecommend();
  const { defaults, loading: defaultsLoading } = useRecommendDefaults();
  const bookmarks = useBookmarks();
  const popular = usePopularProducts();

  const recommendations = data?.recommendations || [];
  const hasResults = searched && !loading && recommendations.length > 0;
  // 만기금액 막대그래프는 그릴 값이 하나라도 있을 때만 오른쪽 상단에 붙인다.
  const showChart =
    hasResults &&
    recommendations.some((item) => item.maturityAmountWon != null);
  const hasPopular = popular.items.length > 0;
  // 그래프·인기 상품 중 하나라도 있으면 오른쪽 사이드바를 편다.
  const showSide = showChart || hasPopular;

  // 인기 상품 클릭 → 검색 페이지로 이동해 그 상품명으로 바로 검색.
  const handleSearchKeyword = (keyword) => {
    navigate(`/dashboard/products/search?q=${encodeURIComponent(keyword)}`);
  };

  return (
    <main className="dash-main products-page">
      <div className="dash-topbar">
        <div>
          <h1>맞춤형 적금 추천</h1>
          <p>
            내 정보를 입력하면 가입 가능한 적금 중 딱 맞는 상품을 골라드려요.
            목돈 예치(예금)는 상품 검색을 이용해 보세요.
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
        <section
          className={`products-results-col${showSide ? " with-side" : ""}`}
        >
          <div className="products-results-main">
            <RecommendResults
              data={data}
              loading={loading}
              error={error}
              searched={searched}
              bookmarks={bookmarks}
            />
          </div>
          {showSide && (
            <div className="products-results-side">
              {showChart && <MaturityChart items={recommendations} />}
              <PopularProducts
                items={popular.items}
                version={popular.version}
                onSelect={handleSearchKeyword}
              />
            </div>
          )}
        </section>
      </div>

      <Toast toast={bookmarks.toast} onClose={bookmarks.clearToast} />
    </main>
  );
}

export default ProductsPage;
