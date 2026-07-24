import { useEffect } from "react";
import { useSearchParams } from "react-router-dom";
import Toast from "../../components/common/Toast";
import ProductsTabNav from "../../components/common/ProductsTabNav";
import { useBookmarks } from "../../hooks/useBookmarks";
import FinancialSearchForm from "./components/FinancialSearchForm";
import FinancialSearchResults from "./components/FinancialSearchResults";
import { useFinancialSearch } from "./hooks/useFinancialSearch";
import "./FinancialSearchPage.css";

function FinancialSearchPage() {
  const search = useFinancialSearch();
  const bookmarks = useBookmarks();
  const [searchParams] = useSearchParams();

  // 인기 검색어 등에서 ?q=로 넘어오면 진입하자마자 그 검색어로 검색한다.
  useEffect(() => {
    const initialQuery = searchParams.get("q");
    if (initialQuery) {
      search.runSearch(initialQuery);
    }
    // 진입 시 1회만 실행.
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  return (
    <main className="dash-main financial-search-page">
      <div className="dash-topbar">
        <div>
          <h1>금융상품 검색</h1>
          <p>
            원하는 조건을 자연어로 입력하면 예금·적금·대출을 한 번에 찾아드려요.
          </p>
        </div>
      </div>

      <ProductsTabNav />

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
        bookmarks={bookmarks}
      />

      <Toast toast={bookmarks.toast} onClose={bookmarks.clearToast} />
    </main>
  );
}

export default FinancialSearchPage;
