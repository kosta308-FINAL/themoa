import { NavLink } from "react-router-dom";
import "./ProductsTabNav.css";

// 금융상품 영역 안에서 "맞춤 추천"과 "상품 검색"을 형제 탭으로 오간다.
// (검색을 추천 밑에 묻지 않으면서도 top-level을 늘리지 않아 정책 검색과 명칭이 겹치지 않게 한다)
const TABS = [
  { to: "/dashboard/products", label: "맞춤 추천", end: true },
  { to: "/dashboard/products/search", label: "상품 검색" },
];

/**
 * 활성 탭에만 초록 알약(pill)을 그리고 view-transition-name을 공유시켜,
 * 탭 전환 시 브라우저가 알약을 다른 탭 위치로 부드럽게 이동(morph)시킨다.
 * (viewTransition 미지원 브라우저는 자동으로 즉시 전환으로 폴백된다)
 */
function ProductsTabNav() {
  return (
    <nav className="products-tabnav" aria-label="금융상품">
      {TABS.map((tab) => (
        <NavLink
          key={tab.to}
          to={tab.to}
          end={tab.end}
          viewTransition
          className={({ isActive }) =>
            `products-tab${isActive ? " active" : ""}`
          }
        >
          {({ isActive }) => (
            <>
              {isActive && <span className="products-tab-pill" />}
              <span className="products-tab-label">{tab.label}</span>
            </>
          )}
        </NavLink>
      ))}
    </nav>
  );
}

export default ProductsTabNav;
