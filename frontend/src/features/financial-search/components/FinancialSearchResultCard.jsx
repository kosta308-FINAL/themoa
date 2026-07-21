const PRODUCT_TYPE_LABELS = {
  DEPOSIT: "정기예금",
  SAVING: "적금",
  MORTGAGE: "주택담보대출",
  RENT: "전세자금대출",
  CREDIT: "개인신용대출",
};

const LOAN_TYPES = new Set(["MORTGAGE", "RENT", "CREDIT"]);

/**
 * 검색 결과 카드 1건. 대표금리 기준이 상품 성격에 따라 달라서(예·적금=최고금리, 대출=최저금리)
 * 라벨도 그에 맞춰 바꿔 표시한다.
 */
function FinancialSearchResultCard({ item }) {
  const typeLabel = PRODUCT_TYPE_LABELS[item.productType] || item.productType;
  const isLoan = LOAN_TYPES.has(item.productType);
  const rateLabel = isLoan ? "최저" : "최고";

  return (
    <article className="fs-card">
      <div className="fs-card-head">
        <span className={`fs-type ${isLoan ? "fs-type-loan" : ""}`}>
          {typeLabel}
        </span>
        {item.representativeRate != null && (
          <span className="fs-rate">
            {rateLabel} <strong>{item.representativeRate}%</strong>
            {item.representativeTermMonth != null &&
              ` · ${item.representativeTermMonth}개월`}
          </span>
        )}
      </div>

      <h3 className="fs-name">
        <span className="fs-company">{item.companyName}</span>
        {item.productName}
      </h3>

      {item.joinMethod && (
        <p className="fs-join">가입방법 · {item.joinMethod}</p>
      )}

      {item.specialCondition && (
        <p className="fs-special">{item.specialCondition}</p>
      )}

      {item.matchReason && (
        <p className="fs-reason">
          <b>왜 나왔나요?</b> {item.matchReason}
        </p>
      )}

      {item.officialUrl && (
        <a
          className="fs-link"
          href={item.officialUrl}
          target="_blank"
          rel="noreferrer"
        >
          🔗 {item.companyName} 사이트로 이동
        </a>
      )}
    </article>
  );
}

export default FinancialSearchResultCard;
