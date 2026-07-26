import { Link } from "react-router-dom";
import DashEmptyState from "./DashEmptyState";
import DashSectionError from "./DashSectionError";
import { formatDateTime, formatRate } from "../dashboardUtils";

const productTypeLabel = (targetType) => {
  if (targetType === "SAVINGS_PRODUCT") return "예·적금";
  if (targetType === "LOAN_PRODUCT") return "대출";
  return "금융상품";
};

function RecommendedProduct({ bookmarks, loading, error }) {
  const items = bookmarks?.items || bookmarks || [];
  const product = items[0];

  return (
    <div className="widget-panel">
      <div className="widget-panel-header">
        <h3>관심 금융상품</h3>
      </div>
      {loading && !bookmarks && <div className="dash-loading">관심 상품을 불러오고 있어요.</div>}
      {error && !bookmarks && <DashSectionError message="관심 금융상품을 불러오지 못했어요." />}
      {!loading && !error && !product && (
        <DashEmptyState
          icon="search"
          title="아직 관심 상품이 없어요"
          description="마음에 드는 예·적금이나 대출 상품을 찜해보세요."
          action={{ label: "금융상품 찾기", to: "/dashboard/products/search" }}
        />
      )}
      {product && (
        <>
          {error && <DashSectionError message={error} />}
          <div className="product-card">
            <span className="product-badge">{productTypeLabel(product.targetType)}</span>
            <h4>{product.title}</h4>
            {product.subtitle && <p className="product-subtitle">{product.subtitle}</p>}
            <div className="product-tags">
              {product.termMonth != null && <span>{product.termMonth}개월</span>}
              {product.createdAt && <span>{formatDateTime(product.createdAt)} 저장</span>}
            </div>
            <div className="product-rate">
              <span>금리</span>
              <strong>{formatRate(product.rate)}</strong>
            </div>
            <Link to="/dashboard/products/search" className="product-cta">
              금융상품 찾기
            </Link>
          </div>
        </>
      )}
    </div>
  );
}

export default RecommendedProduct;
