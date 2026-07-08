import { recommendedProduct } from '../../constants/mockDashboard'

function RecommendedProduct() {
  return (
    <div className="widget-panel">
      <div className="widget-panel-header">
        <h3>AI 추천 금융상품</h3>
        <a href="#">더보기 &gt;</a>
      </div>
      <div className="product-card">
        <span className="product-badge">{recommendedProduct.badge}</span>
        <h4>{recommendedProduct.name}</h4>
        <div className="product-tags">
          {recommendedProduct.tags.map((tag) => (
            <span key={tag}>{tag}</span>
          ))}
        </div>
        <div className="product-rate">
          <span>{recommendedProduct.rateLabel}</span>
          <strong>{recommendedProduct.rate}</strong>
        </div>
        <button type="button" className="product-cta">자세히 보기</button>
      </div>
    </div>
  )
}

export default RecommendedProduct
