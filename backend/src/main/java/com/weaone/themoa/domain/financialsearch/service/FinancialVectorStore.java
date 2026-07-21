package com.weaone.themoa.domain.financialsearch.service;

import org.springframework.ai.vectorstore.VectorStore;

// 금융상품 전용 Qdrant 컬렉션(financial_products)에 연결된 VectorStore를 감싼 래퍼.
// 일부러 VectorStore를 직접 스프링 빈으로 노출하지 않고 이 래퍼 타입으로 감싼다:
// 정책 RAG 코드가 ObjectProvider<VectorStore>로 "이름 없이" 주입받기 때문에, VectorStore 타입 빈이 2개가 되면
// 주입이 모호해져(NoUniqueBeanDefinitionException) 정책 검색이 깨진다. 래퍼로 감싸면 VectorStore 타입 빈은
// 정책용 1개로 유지되므로 정책 코드에 아무 영향이 없다(@Primary도, 정책 빈 재정의도 필요 없음).
public class FinancialVectorStore {

    private final VectorStore delegate;

    public FinancialVectorStore(VectorStore delegate) {
        this.delegate = delegate;
    }

    public VectorStore delegate() {
        return delegate;
    }
}
