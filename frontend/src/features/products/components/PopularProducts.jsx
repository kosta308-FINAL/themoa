import BankBadge from "../../../components/common/BankBadge";

/**
 * 실시간 인기 상품 위젯(북마크 많은 순). 항목을 누르면 onSelect로 상품명을 넘겨
 * 검색 페이지에서 바로 검색한다. 데이터가 없으면 아무것도 렌더하지 않는다.
 * version이 바뀔 때마다(30초 자동 갱신) 목록에 살짝 페이드 애니메이션을 준다.
 */
function PopularProducts({ items, onSelect, version }) {
  if (!items || items.length === 0) {
    return null;
  }

  return (
    <section className="pop-products">
      <h3 className="pop-title">인기 상품</h3>
      <p className="pop-sub">북마크 많은 순</p>
      {/* key에 version을 넣어 갱신 때마다 페이드 애니메이션을 다시 재생한다. */}
      <ol className="pop-list" key={version}>
        {items.map((item, index) => (
          <li key={item.productId ?? `${item.productName}-${index}`}>
            <button type="button" onClick={() => onSelect(item.productName)}>
              <span className="pop-rank">{item.rank ?? index + 1}</span>
              <BankBadge companyName={item.companyName} size={26} />
              <span className="pop-info">
                <span className="pop-name">{item.productName}</span>
                <span className="pop-meta">
                  {item.companyName}
                  {item.rate != null && ` · ${item.rate}%`}
                  {item.termMonth != null && ` · ${item.termMonth}개월`}
                </span>
              </span>
              <span className="pop-bookmark">🔖 {item.bookmarkCount}</span>
            </button>
          </li>
        ))}
      </ol>
    </section>
  );
}

export default PopularProducts;
