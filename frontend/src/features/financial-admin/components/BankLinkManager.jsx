import { useState } from "react";
import { useBankLinks } from "../hooks/useBankLinks";

/**
 * 은행 공식 링크 관리. 링크가 없는 회사는 상품 화면에서 검색 링크로 대체되므로,
 * 그 목록을 보고 하나씩 채워 넣는 흐름으로 구성했다.
 * 회사명은 상품 데이터와 매칭돼야 해서 "링크 없는 회사" 칩을 눌러 그대로 채우는 걸 기본 동선으로 둔다.
 */
function BankLinkManager() {
  const bankLinks = useBankLinks();
  const [companyName, setCompanyName] = useState("");
  const [officialUrl, setOfficialUrl] = useState("");

  const handleSubmit = async (event) => {
    event.preventDefault();
    const saved = await bankLinks.save(companyName, officialUrl);
    if (saved) {
      setCompanyName("");
      setOfficialUrl("");
    }
  };

  const startEdit = (link) => {
    setCompanyName(link.companyName);
    setOfficialUrl(link.officialUrl);
  };

  return (
    <section className="fa-card" id="bank-links">
      <div className="fa-card-head">
        <div>
          <h2>3. 은행 공식 링크 관리</h2>
          <p>
            링크가 등록되지 않은 은행은 상품 화면에서 검색 링크로 대체됩니다.
            아래 목록에서 회사명을 눌러 공식 URL을 채워 주세요.
          </p>
        </div>
      </div>

      {bankLinks.error && (
        <div className="fa-alert fa-alert-danger">{bankLinks.error}</div>
      )}

      <form className="fa-link-form" onSubmit={handleSubmit}>
        <input
          type="text"
          value={companyName}
          onChange={(event) => setCompanyName(event.target.value)}
          placeholder="회사명 (상품 데이터와 동일하게)"
        />
        <input
          type="text"
          value={officialUrl}
          onChange={(event) => setOfficialUrl(event.target.value)}
          placeholder="https://www.example.com/"
        />
        <button
          type="submit"
          className="admin-btn fa-btn-primary"
          disabled={bankLinks.saving}
        >
          {bankLinks.saving ? "저장 중…" : "저장"}
        </button>
      </form>

      {bankLinks.loading ? (
        <p className="fa-note">은행 링크를 불러오고 있어요.</p>
      ) : (
        <>
          <div className="fa-link-block">
            <h3>
              링크 없는 은행
              <span className="fa-count">
                {bankLinks.companiesWithoutLink.length}
              </span>
            </h3>
            {bankLinks.companiesWithoutLink.length === 0 ? (
              <p className="fa-note">모든 은행에 공식 링크가 등록돼 있어요.</p>
            ) : (
              <div className="fa-chips">
                {bankLinks.companiesWithoutLink.map((company) => (
                  <button
                    key={company}
                    type="button"
                    className="fa-chip"
                    onClick={() => setCompanyName(company)}
                  >
                    {company}
                  </button>
                ))}
              </div>
            )}
          </div>

          <div className="fa-link-block">
            <h3>
              등록된 링크
              <span className="fa-count">{bankLinks.links.length}</span>
            </h3>
            {bankLinks.links.length === 0 ? (
              <p className="fa-note">아직 등록된 링크가 없어요.</p>
            ) : (
              <ul className="fa-link-list">
                {bankLinks.links.map((link) => (
                  <li key={link.companyName}>
                    <div className="fa-link-info">
                      <strong>{link.companyName}</strong>
                      <a
                        href={link.officialUrl}
                        target="_blank"
                        rel="noreferrer"
                      >
                        {link.officialUrl}
                      </a>
                    </div>
                    <div className="fa-link-actions">
                      <button
                        type="button"
                        className="admin-btn admin-btn-secondary"
                        onClick={() => startEdit(link)}
                      >
                        수정
                      </button>
                      <button
                        type="button"
                        className="admin-btn fa-btn-danger"
                        disabled={bankLinks.busyCompany === link.companyName}
                        onClick={() => bankLinks.remove(link.companyName)}
                      >
                        {bankLinks.busyCompany === link.companyName
                          ? "삭제 중…"
                          : "삭제"}
                      </button>
                    </div>
                  </li>
                ))}
              </ul>
            )}
          </div>
        </>
      )}
    </section>
  );
}

export default BankLinkManager;
