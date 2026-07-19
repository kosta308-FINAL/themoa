import { useEffect, useRef, useState } from "react";
import { searchMerchantAliases } from "../../../api/merchantAliasApi";

/** F-03 가맹점 검색/선택/새로만들기(view/fixedExpense.md §3.3, 카드형 전용). */
function MerchantAliasPicker({ initialName = "", onChange }) {
  const [query, setQuery] = useState(initialName);
  const [suggestions, setSuggestions] = useState([]);
  const [isOpen, setIsOpen] = useState(false);
  const boxRef = useRef(null);

  useEffect(() => {
    const timer = setTimeout(() => {
      searchMerchantAliases(query)
        .then(setSuggestions)
        .catch(() => setSuggestions([]));
    }, 200);
    return () => clearTimeout(timer);
  }, [query]);

  useEffect(() => {
    const handleOutsideClick = (event) => {
      if (boxRef.current && !boxRef.current.contains(event.target)) setIsOpen(false);
    };
    document.addEventListener("mousedown", handleOutsideClick);
    return () => document.removeEventListener("mousedown", handleOutsideClick);
  }, []);

  const handleInput = (event) => {
    const next = event.target.value;
    setQuery(next);
    setIsOpen(true);
    onChange({ merchantAliasId: null, name: next });
  };

  const selectAlias = (alias) => {
    setQuery(alias.canonicalServiceName);
    setIsOpen(false);
    onChange({ merchantAliasId: alias.id, name: alias.canonicalServiceName });
  };

  const trimmedQuery = query.trim();
  const hasExactMatch = suggestions.some(
    (alias) =>
      alias.canonicalServiceName.toLowerCase() === trimmedQuery.toLowerCase(),
  );

  return (
    <div className="fx-merchant-picker" ref={boxRef}>
      <input
        value={query}
        onChange={handleInput}
        onFocus={() => setIsOpen(true)}
        placeholder="검색하거나 새 서비스 이름을 입력하세요"
        autoComplete="off"
        required
      />
      {isOpen && (
        <ul className="fx-merchant-picker-list">
          {suggestions.map((alias) => (
            <li key={alias.id}>
              <button type="button" onClick={() => selectAlias(alias)}>
                {alias.canonicalServiceName}
              </button>
            </li>
          ))}
          {trimmedQuery && !hasExactMatch && (
            <li>
              <button
                type="button"
                className="fx-merchant-picker-new"
                onClick={() => {
                  setIsOpen(false);
                  onChange({ merchantAliasId: null, name: trimmedQuery });
                }}
              >
                + &quot;{trimmedQuery}&quot; 새로 만들기
              </button>
            </li>
          )}
          {!trimmedQuery && !suggestions.length && (
            <li className="fx-merchant-picker-empty">
              서비스 이름을 입력해보세요
            </li>
          )}
        </ul>
      )}
    </div>
  );
}

export default MerchantAliasPicker;
