package com.weaone.themoa.domain.financialsearch.service;

import com.weaone.themoa.domain.financialsearch.repository.FinancialLoanSearchRepository;
import com.weaone.themoa.domain.financialsearch.repository.FinancialSavingsSearchRepository;
import com.weaone.themoa.domain.recommend.entity.LoanProduct;
import com.weaone.themoa.domain.recommend.entity.SavingsProduct;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

// DB에 있는 전체 금융상품을 Qdrant(금융 전용 컬렉션)에 임베딩해서 채워 넣는 배치.
// 컨트롤러의 /embeddings/rebuild로 수동 트리거된다(자동 스케줄링은 아직 없음).
// 상품 엔티티/레포지토리는 recommend 도메인 것을 재사용하고, 벡터스토어는 정책과 분리된 FinancialVectorStore를 쓴다.
@Service
public class FinancialEmbeddingService {

    // 한 번에 Qdrant로 보내는 문서 개수. 너무 크면 요청 하나가 무거워지고, 너무 작으면 API 호출 횟수가 늘어난다.
    private static final int BATCH_SIZE = 100;

    private final FinancialSavingsSearchRepository savingsProductRepository;
    private final FinancialLoanSearchRepository loanProductRepository;
    private final FinancialDocumentBuilder documentBuilder;
    private final ObjectProvider<FinancialVectorStore> financialVectorStoreProvider;

    public FinancialEmbeddingService(FinancialSavingsSearchRepository savingsProductRepository,
                                     FinancialLoanSearchRepository loanProductRepository,
                                     FinancialDocumentBuilder documentBuilder,
                                     ObjectProvider<FinancialVectorStore> financialVectorStoreProvider) {
        this.savingsProductRepository = savingsProductRepository;
        this.loanProductRepository = loanProductRepository;
        this.documentBuilder = documentBuilder;
        this.financialVectorStoreProvider = financialVectorStoreProvider;
    }

    // LoanProduct.getOptions()가 LAZY라서, documentBuilder.build()에서 옵션(담보/상환유형 등)을
    // 읽으려면 트랜잭션(영속성 컨텍스트)이 열려 있어야 한다.
    @Transactional(readOnly = true)
    public int embedAll() {
        FinancialVectorStore financialVectorStore = financialVectorStoreProvider.getIfAvailable();
        if (financialVectorStore == null) {
            throw new IllegalStateException(
                    "금융 Qdrant VectorStore가 비활성화되어 있습니다. FINANCIAL_RAG_ENABLED=true 와 Qdrant 기동 여부를 확인하세요.");
        }
        VectorStore vectorStore = financialVectorStore.delegate();
        int count = 0;
        List<Document> batch = new ArrayList<>(BATCH_SIZE);
        for (SavingsProduct product : savingsProductRepository.findAll()) {
            batch.add(documentBuilder.build(product));
            count++;
            if (batch.size() >= BATCH_SIZE) {
                vectorStore.add(batch);
                batch.clear();
            }
        }
        for (LoanProduct product : loanProductRepository.findAll()) {
            batch.add(documentBuilder.build(product));
            count++;
            if (batch.size() >= BATCH_SIZE) {
                vectorStore.add(batch);
                batch.clear();
            }
        }
        if (!batch.isEmpty()) {
            vectorStore.add(batch);
        }
        return count;
    }
}
