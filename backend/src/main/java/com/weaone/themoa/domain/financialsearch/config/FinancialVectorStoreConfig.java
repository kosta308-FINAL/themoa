package com.weaone.themoa.domain.financialsearch.config;

import com.weaone.themoa.domain.financialsearch.service.FinancialVectorStore;
import io.qdrant.client.QdrantClient;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.qdrant.QdrantVectorStore;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 금융상품 검색 전용 VectorStore(financial_products 컬렉션)를 만든다.
 *
 * <p>안전 설계(팀원=정책 RAG 무피해):
 * <ul>
 *   <li>QdrantVectorStore(=VectorStore)를 그대로 @Bean으로 노출하지 않고 {@link FinancialVectorStore}로 감싸서
 *       등록한다. 그래야 정책 코드의 {@code ObjectProvider<VectorStore>} 주입이 여전히 정책용 1개로 유일하게 유지된다.</li>
 *   <li>QdrantClient / EmbeddingModel은 기존(정책 RAG용) 자동설정 빈을 "읽기만" 해서 재사용한다.
 *       같은 Qdrant 서버 안에 별도 컬렉션만 추가로 쓰는 것이라 정책 컬렉션(youthcenter_policies)·설정에 영향이 없다.</li>
 *   <li>app.financial.rag.enabled=true 일 때만 이 설정이 활성화된다(꺼져 있으면 벡터스토어 없이 LIKE 폴백).
 *       Qdrant/임베딩 인프라가 아직 안 떠 있으면 null을 반환해, 그 경우에도 검색 서비스가 LIKE로 폴백하도록 한다.</li>
 * </ul>
 */
@Configuration
@ConditionalOnProperty(prefix = "app.financial.rag", name = "enabled", havingValue = "true")
public class FinancialVectorStoreConfig {

    @Bean
    public FinancialVectorStore financialVectorStore(ObjectProvider<QdrantClient> qdrantClientProvider,
                                                     ObjectProvider<EmbeddingModel> embeddingModelProvider,
                                                     FinancialRagProperties properties) {
        QdrantClient qdrantClient = qdrantClientProvider.getIfAvailable();
        EmbeddingModel embeddingModel = embeddingModelProvider.getIfAvailable();
        if (qdrantClient == null || embeddingModel == null) {
            // Qdrant(정책 RAG 자동설정으로 켜짐) 또는 임베딩 모델이 아직 없으면 벡터검색을 포기하고 LIKE 폴백에 맡긴다.
            return null;
        }
        QdrantVectorStore delegate = QdrantVectorStore.builder(qdrantClient, embeddingModel)
                .collectionName(properties.getCollectionName())
                .initializeSchema(true)   // 금융 전용 컬렉션이 없으면 생성(정책 컬렉션과 별개)
                .build();
        try {
            // 이 QdrantVectorStore는 스프링 빈이 아니라 래퍼 안에 들어가므로, 스프링이 afterPropertiesSet()을
            // 자동 호출해주지 않는다. 컬렉션 생성(initializeSchema)이 여기서 일어나므로 직접 호출해야 한다.
            // (호출하지 않으면 컬렉션이 이미 있는 환경에서만 동작하고, 새 Qdrant 볼륨에선 컬렉션이 없어 검색이 실패한다.)
            delegate.afterPropertiesSet();
        } catch (Exception e) {
            throw new IllegalStateException("금융상품 Qdrant 컬렉션을 초기화하지 못했습니다.", e);
        }
        return new FinancialVectorStore(delegate);
    }
}
