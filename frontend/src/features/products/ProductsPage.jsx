import RecommendForm from "./components/RecommendForm";
import RecommendResults from "./components/RecommendResults";
import { useRecommend } from "./hooks/useRecommend";
import "./ProductsPage.css";

function ProductsPage() {
  const { data, loading, error, searched, runRecommend } = useRecommend();

  return (
    <main className="dash-main products-page">
      <div className="dash-topbar">
        <div>
          <h1>맞춤형 금융상품 추천</h1>
          <p>
            내 정보를 입력하면 가입 가능한 예·적금 중 딱 맞는 상품을 골라드려요.
          </p>
        </div>
      </div>

      <div className="products-layout">
        <RecommendForm loading={loading} onSubmit={runRecommend} />
        <section className="products-results-col">
          <RecommendResults
            data={data}
            loading={loading}
            error={error}
            searched={searched}
          />
        </section>
      </div>
    </main>
  );
}

export default ProductsPage;
