import { Link } from "react-router-dom";
import FinancialSearchForm from "./components/FinancialSearchForm";
import FinancialSearchResults from "./components/FinancialSearchResults";
import { useFinancialSearch } from "./hooks/useFinancialSearch";
import "./FinancialSearchPage.css";

function FinancialSearchPage() {
  const search = useFinancialSearch();

  return (
    <main className="dash-main financial-search-page">
      <div className="dash-topbar">
        <div>
          <h1>금융상품 검색</h1>
          <p>
            원하는 조건을 자연어로 입력하면 예금·적금·대출을 한 번에 찾아드려요.
          </p>
        </div>
        <Link className="fs-nav-link" to="/dashboard/products">
          맞춤형 추천 받기 →
        </Link>
      </div>

      <FinancialSearchForm
        query={search.query}
        sort={search.sort}
        loading={search.loading}
        searched={search.searched}
        onQueryChange={search.setQuery}
        onSortChange={search.setSort}
        onSearch={search.runSearch}
      />

      <FinancialSearchResults
        data={search.data}
        loading={search.loading}
        error={search.error}
        searched={search.searched}
        onSearch={search.runSearch}
      />
    </main>
  );
}

export default FinancialSearchPage;
