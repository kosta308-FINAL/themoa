package com.weaone.themoa.domain.customerservice.rag;

import com.weaone.themoa.config.CustomerServiceRagProperties;
import io.qdrant.client.QdrantClient;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.qdrant.QdrantVectorStore;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

@Component
public class CustomerServiceVectorStoreFactory {

    private final ObjectProvider<QdrantClient> qdrantClientProvider;
    private final ObjectProvider<EmbeddingModel> embeddingModelProvider;
    private final CustomerServiceRagProperties properties;
    private VectorStore vectorStore;

    public CustomerServiceVectorStoreFactory(ObjectProvider<QdrantClient> qdrantClientProvider,
                                             ObjectProvider<EmbeddingModel> embeddingModelProvider,
                                             CustomerServiceRagProperties properties) {
        this.qdrantClientProvider = qdrantClientProvider;
        this.embeddingModelProvider = embeddingModelProvider;
        this.properties = properties;
    }

    public synchronized VectorStore getIfAvailable() {
        if (vectorStore != null) {
            return vectorStore;
        }
        QdrantClient qdrantClient = qdrantClientProvider.getIfAvailable();
        EmbeddingModel embeddingModel = embeddingModelProvider.getIfAvailable();
        if (qdrantClient == null || embeddingModel == null) {
            return null;
        }
        vectorStore = QdrantVectorStore.builder(qdrantClient, embeddingModel)
                .collectionName(properties.collectionName())
                .initializeSchema(true)
                .build();
        return vectorStore;
    }
}
